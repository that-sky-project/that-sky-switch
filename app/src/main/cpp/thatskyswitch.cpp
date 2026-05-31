#include <dlfcn.h>
#include <android/log.h>
#include <cstring>
#include <cstdlib>
#include <unistd.h>
#include <pthread.h>
#include <link.h>
#include "shadowhook.h"
#include "util.h"

// original function type
typedef bool (*orig_ca_func_t)(int);

static orig_ca_func_t orig_ca_func = nullptr;

// the verify cert function need return true
static bool fake_verify_cert(int a) {
    return true;
}

void hook_verify_cert(void * target_addr) {
    if (!target_addr) {
        LOGI("❌ libBootloader.so not loaded yet");
        return;
    }

    LOGI("  hooking address: %p", (void *) target_addr);

    // Hook
    shadowhook_hook_func_addr(
            (void *) target_addr,             // target
            (void *) fake_verify_cert,        // replacement
            (void **) &orig_ca_func           // backup
    );
}


static void *worker_thread(void *) {
    LOGI("worker thread started");

    // let's wait few seconds
    sleep(3);

    uintptr_t target = 0;

    auto result = get_module_base(&target);

    if (result != 1) {
        LOGI("Failed to find libBootloader.so base. Status code: %d, target address: 0x%lx", result, target);
        return nullptr;
    }

    hook_verify_cert((void *)target);

    return nullptr;
}




// This function will run when shadowhook lib loaded.
__attribute__((constructor)) static void init_hook() {
    // 初始化 ShadowHook
    pthread_t tid;
    shadowhook_init(SHADOWHOOK_MODE_UNIQUE, true);  // debug 开启日志


    if (pthread_create(&tid, nullptr, worker_thread, nullptr) == 0) {
        pthread_detach(tid);
        LOGI("worker thread created");
    } else {
        LOGI("failed to create worker thread");
    }
}
