#include <cstdio>
#include <cstdint>
#include <cstring>
#include <link.h>
#include <cstdlib>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define TAG "ThatSkySwitch"
#define WC (-1)
#define MAX_SEGS 64

typedef struct {
    uintptr_t base;
    size_t    size;
} Segment;

typedef struct {
    const char *name;
    uintptr_t   base;
    uintptr_t   end;
} ModInfo;

static int mod_cb(struct dl_phdr_info *info, size_t sz, void *data) {
    auto *m = static_cast<ModInfo *>(data);
    const char *bn = strrchr(info->dlpi_name, '/');
    bn = bn ? bn + 1 : info->dlpi_name;
    if (strcmp(bn, m->name) != 0) return 0;

    m->base = (uintptr_t)info->dlpi_addr;
    for (int i = 0; i < info->dlpi_phnum; i++) {
        const ElfW(Phdr) *ph = &info->dlpi_phdr[i];
        if (ph->p_type != PT_LOAD) continue;
        uintptr_t e = m->base + ph->p_vaddr + ph->p_memsz;
        if (e > m->end) m->end = e;
    }
    return 1;
}

static int collect_readable_segs(const char *soname,
                                 Segment *segs, int max_segs) {
    ModInfo mod = { soname, 0, 0 };
    dl_iterate_phdr(mod_cb, &mod);
    if (!mod.base) {
        LOGI("dl_iterate_phdr not found %s", soname);
        return 0;
    }
    LOGI("mod range: 0x%lx - 0x%lx", mod.base, mod.end);

    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) {
        LOGI("cannot open /proc/self/maps");
        return 0;
    }

    int count = 0;
    char line[512];
    while (fgets(line, sizeof(line), f) && count < max_segs) {
        uintptr_t start, end;
        char perms[8];
        unsigned long offset;
        int dm, dn;
        unsigned long inode;

        if (sscanf(line, "%lx-%lx %7s %lx %x:%x %lu",
                   &start, &end, perms, &offset, &dm, &dn, &inode) < 7) continue;

        if (start < mod.base || start >= mod.end) continue;
        if (perms[0] != 'r') continue;

        segs[count].base = start;
        segs[count].size = end - start;
        LOGI("segment[%d]: 0x%lx size=0x%zx perms=%s", count, start, end - start, perms);
        count++;
    }
    fclose(f);
    return count;
}

static uintptr_t *pattern_scan_segs(const Segment *segs, int seg_count,
                                    const int16_t *pat, size_t pat_len,
                                    size_t *out_count) {
    size_t cap = 64;
    auto *addrs = static_cast<uintptr_t *>(malloc(cap * sizeof(uintptr_t)));
    if (!addrs) { *out_count = 0; return nullptr; }
    size_t count = 0;

    for (int s = 0; s < seg_count; s++) {
        const auto *mem = reinterpret_cast<const uint8_t *>(segs[s].base);
        size_t msz = segs[s].size;
        if (msz < pat_len) continue;

        for (size_t i = 0; i <= msz - pat_len; i++) {
            int ok = 1;
            for (size_t j = 0; j < pat_len; j++) {
                if (pat[j] == WC) continue;
                if (mem[i + j] != static_cast<uint8_t>(pat[j])) { ok = 0; break; }
            }
            if (!ok) continue;

            if (count >= cap) {
                cap *= 2;
                auto *tmp = static_cast<uintptr_t *>(realloc(addrs, cap * sizeof(uintptr_t)));
                if (!tmp) { free(addrs); *out_count = 0; return nullptr; }
                addrs = tmp;
            }
            addrs[count++] = segs[s].base + i;
        }
    }

    *out_count = count;
    return addrs;
}

int get_module_base(uintptr_t *target) {
    static const int16_t pattern[] = {
            0xfd,0x7b,0xbd,0xa9, 0xf5,0x0b,0x00,0xf9,
            0xf4,0x4f,0x02,0xa9, 0xfd,0x03,0x00,0x91,
            0xf3,0x03,0x00,0xaa, 0x00,0x04,0x40,0xf9,
            0x40,0x01,0x00,0xb4, 0x68,0x4e,0x40,0xf9
    };
    const size_t pat_len = sizeof(pattern) / sizeof(pattern[0]);

    Segment segs[MAX_SEGS];
    int seg_count = collect_readable_segs("libBootloader.so", segs, MAX_SEGS);
    if (seg_count == 0) {
        LOGI("can not found readable segment of libBootloader.so");
        return 0;
    }

    size_t count = 0;
    uintptr_t *hits = pattern_scan_segs(segs, seg_count, pattern, pat_len, &count);
    if (!hits) {
        LOGI("pattern_scan_segs require memory failed");
        return -1;
    }

    LOGI("found %zu matched", count);
    int result = 0;
    for (size_t i = 0; i < count; i++) {
        LOGI("  [%zu] 0x%lx  (offset 0x%lx)", i, hits[i], hits[i] - segs[0].base);
        *target = hits[i];
        result++;
    }

    free(hits);
    return result;
}