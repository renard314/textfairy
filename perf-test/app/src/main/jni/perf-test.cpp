#include <jni.h>
#include "PixBinarizer.h"
#include <android/log.h>

#define LOG_TAG "PerfTest"


extern "C" {
JNIEXPORT jstring JNICALL Java_ndk_renard_com_perftest_MainActivity_stringFromJNI(JNIEnv *env,
                                                                                  jobject obj,
                                                                                  jstring filePathJni);
};


JNIEXPORT jstring JNICALL
Java_ndk_renard_com_perftest_MainActivity_stringFromJNI(JNIEnv *env, jobject instance,
                                                        jstring filePathJni) {
    PixBinarizer binarizer(false);

    const char *s = env->GetStringUTFChars(filePathJni, NULL);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "loading %s\n", s);
    std::string filePath = s;
    env->ReleaseStringUTFChars(filePathJni, s);
    Pix *pixOrg = pixRead("/mnt/sdcard/test_image.png");
    if (pixOrg != NULL) {
        Pix *pixBinary = binarizer.binarize(pixOrg, NULL);
        pixDestroy(&pixBinary);
        pixDestroy(&pixOrg);
    } else {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "cant read pix");
    }

#if defined(__arm__)
#if defined(__ARM_ARCH_7A__)
#if defined(__ARM_NEON__)
#if defined(__ARM_PCS_VFP)
  #define ABI "armeabi-v7a/NEON (hard-float)"
#else
  #define ABI "armeabi-v7a/NEON"
#endif
#else
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a (hard-float)"
#else
#define ABI "armeabi-v7a"
#endif
#endif
#else
#define ABI "armeabi"
#endif
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif
    return env->NewStringUTF("Hello from JNI !  Compiled with ABI " ABI ".");
}