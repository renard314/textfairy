/*
 * Copyright (C) 2012,2013 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include "image_processing.h"
#include <cstring>
#include "android/bitmap.h"
#include "allheaders.h"
#include <sstream>
#include <iostream>
#include <pthread.h>
#include <cmath>
#include "pageseg.h"
#include "PixBlurDetect.h"
#include "pixFunc.hpp"
#include "combine_pixa.h"
#include "ProgressCallback.h"
#include <fcntl.h>
#include <sstream>
#include <cmath>


using namespace std;

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */

    static jmethodID onProgressImage, onProgressText, onLayoutElements;
    static jfieldID field_mNativeData;
    static JavaVM* g_vm;


    class native_data_t : public ProgressCallback{

    public:
        virtual void sendMessage(int message) {
            JNIEnv *env;
            bool needDetach = false;
            // double check it's all ok
            int getEnvStat = g_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
            if (getEnvStat == JNI_EDETACHED) {
                LOGW("GetEnv: not attached");
                needDetach = true;
                if (g_vm->AttachCurrentThread(&env, NULL) != 0) {
                    LOGE("Failed to attach");
                    return;
                }
            }

            env->CallVoidMethod(cachedObject, onProgressText, message);

            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
            }

            if (needDetach) {
                g_vm->DetachCurrentThread();
            }
        }

        virtual void sendPix(Pix* pix) {
            JNIEnv *env;
            bool needDetach = false;
            // double check it's all ok
            int getEnvStat = g_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
            if (getEnvStat == JNI_EDETACHED) {
                LOGW("GetEnv: not attached");
                needDetach = true;
                if (g_vm->AttachCurrentThread(&env, NULL) != 0) {
                    LOGE("Failed to attach");
                    return;
                }
            }

            env->CallVoidMethod(cachedObject, onProgressImage, (jlong) pix);

            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
            }

            if (needDetach) {
                g_vm->DetachCurrentThread();
            }
        }

        jobject cachedObject;

        ~native_data_t(){
            JNIEnv *env;
            bool needDetach = false;
            int getEnvStat = g_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
            if (getEnvStat == JNI_EDETACHED) {
                LOGW("GetEnv: not attached");
                needDetach = true;
                if (g_vm->AttachCurrentThread(&env, NULL) != 0) {
                    LOGE("Failed to attach");
                    return;
                }
            }

            env->DeleteGlobalRef(cachedObject);

            if (needDetach) {
                g_vm->DetachCurrentThread();
            }

        }
    };


    static inline native_data_t * get_native_data(JNIEnv *env, jobject object) {
        return (native_data_t *) (env->GetLongField(object, field_mNativeData));
    }

    jint JNI_OnLoad(JavaVM* vm, void* reserved) {
        g_vm = vm;
        return JNI_VERSION_1_6;
    }


    void Java_com_googlecode_tesseract_android_NativeBinding_nativeClassInit(JNIEnv *env, jobject _thiz) {
        jclass cls = env->FindClass("com/googlecode/tesseract/android/NativeBinding");
        onProgressImage = env->GetMethodID(cls, "onProgressImage", "(J)V");
        onProgressText = env->GetMethodID(cls, "onProgressText", "(I)V");
        onLayoutElements = env->GetMethodID(cls, "onLayoutElements", "(JJ)V");
        field_mNativeData = env->GetFieldID(cls, "mNativeData", "J");
    }

    void JNI_OnUnload(JavaVM *vm, void *reserved) {
    }


    void Java_com_googlecode_tesseract_android_NativeBinding_nativeConstruct(JNIEnv* env, jobject object) {

        native_data_t *nat = new native_data_t;

        nat->cachedObject = env->NewGlobalRef(object);

        env->SetLongField(object, field_mNativeData, (jlong) nat);
    }

    void Java_com_googlecode_tesseract_android_NativeBinding_nativeDestruct(JNIEnv* env, jobject object) {
        native_data_t *nat = (native_data_t *) (env->GetLongField(object, field_mNativeData));
        delete nat;
    }

    jlongArray Java_com_googlecode_tesseract_android_NativeBinding_combineSelectedPixa(JNIEnv *env, jobject thiz, jlong nativePixaText, jlong nativePixaImage, jintArray selectedTexts, jintArray selectedImages) {
        LOGV(__FUNCTION__);
        Pixa *pixaTexts = (PIXA *) nativePixaText;
        Pixa *pixaImages = (PIXA *) nativePixaImage;
        jint* textindexes = env->GetIntArrayElements(selectedTexts, NULL);
        jsize textCount = env->GetArrayLength(selectedTexts);
        jint* imageindexes = env->GetIntArrayElements(selectedImages, NULL);
        jsize imageCount = env->GetArrayLength(selectedImages);

        Pix* pixFinal;
        Pix* pixOcr;
        Boxa* boxaColumns;

        native_data_t *nat = get_native_data(env, thiz);

        combineSelectedPixa(pixaTexts, pixaImages, textindexes, textCount, imageindexes, imageCount, nat, &pixFinal, &pixOcr, &boxaColumns, false);
        nat->sendPix(pixFinal);

        jlongArray result;
        result = env->NewLongArray(3);
        if (result == NULL) {
            return NULL; /* out of memory error thrown */
        }

        jlong fill[3];
        fill[0] = (jlong) pixFinal;
        fill[1] = (jlong) pixOcr;
        fill[2] = (jlong) boxaColumns;
        // move from the temp structure to the java structure
        env->SetLongArrayRegion(result, 0, 3, fill);

        env->ReleaseIntArrayElements(selectedTexts, textindexes, 0);
        env->ReleaseIntArrayElements(selectedImages, imageindexes, 0);
        return result;
    }



    void Java_com_googlecode_tesseract_android_NativeBinding_nativeAnalyseLayout(JNIEnv *env, jobject thiz, jlong nativePix) {
        LOGV(__FUNCTION__);
        Pix *pixOrg = (PIX *) nativePix;
        Pix* pixTextlines = NULL;
        Pixa* pixaTexts, *pixaImages;
        Pix* pixb, *pixhm;

        native_data_t *nat = get_native_data(env, thiz);

        nat->sendMessage(MESSAGE_IMAGE_DETECTION);

        pixb = pixPrepareLayoutAnalysis(pixOrg, nat);
        
        nat->sendPix(pixb);

        segmentComplexLayout(pixOrg, NULL, pixb, &pixaImages, &pixaTexts, nat, false);
        
        pixDestroy(&pixb);

        env->CallVoidMethod(thiz, onLayoutElements, (jlong)pixaTexts, (jlong)pixaImages);

    }

    jobject Java_com_renard_ocr_cropimage_image_1processing_Blur_nativeBlurDetect(JNIEnv *env, jobject thiz, jlong nativePix) {
        Pix *pixOrg = (PIX *) nativePix;
        PixBlurDetect blurDetector(false);
        l_float32 blurValue;
        L_TIMER timer = startTimerNested();
        Box* maxBlurLoc = NULL;
        Pix* pixBlended = blurDetector.makeBlurIndicator(pixOrg,&blurValue, &maxBlurLoc);
        l_int32 w,h,x,y;
        boxGetGeometry(maxBlurLoc,&x,&y,&w,&h);
        //pixRenderBox(pixBlended,maxBlurLoc,2,L_SET_PIXELS);
        //create result
        jclass cls = env->FindClass("com/renard/ocr/cropimage/image_processing/BlurDetectionResult");
        jmethodID constructor = env->GetMethodID(cls, "<init>", "(JDJ)V");
        return env->NewObject(cls, constructor, (jlong)pixBlended, (jdouble)blurValue, (jlong)maxBlurLoc);
    }

    jlong Java_com_googlecode_tesseract_android_NativeBinding_nativeOCRBook(JNIEnv *env, jobject thiz, jlong nativePix) {
        LOGV(__FUNCTION__);
        Pix *pixOrg = (PIX *) nativePix;

        native_data_t *nat = get_native_data(env, thiz);

        Pix* pixText = pixPrepareForOcr(pixOrg, nat);

        return (jlong)pixText;

    }

#ifdef __cplusplus
}
#endif  /* __cplusplus */
