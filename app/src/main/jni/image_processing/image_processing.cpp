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
#include "common.h"
#include <cstring>
#include "android/bitmap.h"
#include "allheaders.h"
#include <sstream>
#include <iostream>
#include <pthread.h>
#include <cmath>
#include "pageseg.h"
#include "PixBlurDetect.h"
#include "PixBinarizer.h"
#include <image_processing_util.h>
#include "SkewCorrector.h"
#include <fcntl.h>

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */
    
    static jmethodID onProgressImage, onProgressValues, onProgressText, onLayoutElements, onUTF8Result, onLayoutPix;
    
    static JNIEnv *cachedEnv;
    static jobject* cachedObject;
    static FILE *inputFile;
    static int pipes[2];


    jint JNI_OnLoad(JavaVM* vm, void* reserved) {
        return JNI_VERSION_1_6;
    }

    
    void Java_com_googlecode_tesseract_android_OCR_nativeInit(JNIEnv *env, jobject _thiz) {
        jclass cls = env->FindClass("com/googlecode/tesseract/android/OCR");
        onProgressImage = env->GetMethodID(cls, "onProgressImage", "(J)V");
        onProgressText = env->GetMethodID(cls, "onProgressText", "(I)V");
        onLayoutElements = env->GetMethodID(cls, "onLayoutElements", "(II)V");
        onUTF8Result = env->GetMethodID(cls, "onUTF8Result", "(Ljava/lang/String;)V");
        onLayoutPix = env->GetMethodID(cls, "onLayoutPix", "(J)V");
        
    }
    jstring Java_com_googlecode_tesseract_android_OCR_stopCaptureLogs(JNIEnv *env, jobject _thiz) {
        char readBuffer[256];
        std:stringstream logbuffer;
        while (fgets(readBuffer, sizeof readBuffer, inputFile) != NULL) {
            logbuffer<<readBuffer;
            __android_log_print(ANDROID_LOG_ERROR, "stderr", readBuffer);
        }

        close(pipes[0]);
        fclose(inputFile);
        const std::string& tmp = logbuffer.str();
        const char* cstr = tmp.c_str();
        return env->NewStringUTF(cstr);
    }

    void Java_com_googlecode_tesseract_android_OCR_startCaptureLogs(JNIEnv *env, jobject _thiz) {
        int lWriteFD = dup(STDERR_FILENO);
        if ( lWriteFD < 0 )
        {
            LOGE("Unable to get STDERR file descriptor.");
            return;
        }

        pipe(pipes);
        dup2(pipes[1], STDERR_FILENO);
        inputFile = fdopen(pipes[0], "r");

        close(pipes[1]);

        int fd = fileno(inputFile);
        int flags = fcntl(fd, F_GETFL, 0);
        flags |= O_NONBLOCK;
        fcntl(fd, F_SETFL, flags);

        if ( 0 == inputFile ) {
            LOGE("Unable to get read pipe for STDERR");
            return;
        }
    }
    
    void JNI_OnUnload(JavaVM *vm, void *reserved) {
    }
    
    void initStateVariables(JNIEnv* env, jobject *object) {
        cachedEnv = env;
        cachedObject = object;
    }
    
    void resetStateVariables() {
        cachedEnv = NULL;
        cachedObject = NULL;
    }
    
    bool isStateValid() {
        if (cachedEnv != NULL && cachedObject != NULL) {
            return true;
        } else {
            LOGI("state is cancelled");
            return false;
            
        }
    }
    
    void messageJavaCallback(int message) {
        if (isStateValid()) {
            cachedEnv->CallVoidMethod(*cachedObject, onProgressText, message);
        }
    }
    
    void pixJavaCallback(Pix* pix) {
        if (isStateValid()) {
            cachedEnv->CallVoidMethod(*cachedObject, onProgressImage, (jlong) pix);
        }
    }
    
    void callbackLayout(const Pix* pixpreview) {
        if (isStateValid()) {
            cachedEnv->CallVoidMethod(*cachedObject, onLayoutPix, (jlong)pixpreview);
        }
        messageJavaCallback(MESSAGE_ANALYSE_LAYOUT);
    }
    
    
    jlongArray Java_com_googlecode_tesseract_android_OCR_combineSelectedPixa(JNIEnv *env, jobject thiz, jlong nativePixaText, jlong nativePixaImage, jintArray selectedTexts, jintArray selectedImages) {
        LOGV(__FUNCTION__);
        Pixa *pixaTexts = (PIXA *) nativePixaText;
        Pixa *pixaImages = (PIXA *) nativePixaImage;
        initStateVariables(env, &thiz);
        jint* textindexes = env->GetIntArrayElements(selectedTexts, NULL);
        jsize textCount = env->GetArrayLength(selectedTexts);
        jint* imageindexes = env->GetIntArrayElements(selectedImages, NULL);
        jsize imageCount = env->GetArrayLength(selectedImages);
        
        Pix* pixFinal;
        Pix* pixOcr;
        Boxa* boxaColumns;
        
        combineSelectedPixa(pixaTexts, pixaImages, textindexes, textCount, imageindexes, imageCount, messageJavaCallback, &pixFinal, &pixOcr, &boxaColumns, true);
        pixJavaCallback(pixFinal);
        
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
        
        resetStateVariables();
        env->ReleaseIntArrayElements(selectedTexts, textindexes, 0);
        env->ReleaseIntArrayElements(selectedImages, imageindexes, 0);
        return result;
    }
    
    
    
    jint Java_com_googlecode_tesseract_android_OCR_nativeAnalyseLayout(JNIEnv *env, jobject thiz, jint nativePix) {
        LOGV(__FUNCTION__);
        Pix *pixOrg = (PIX *) nativePix;
        Pix* pixTextlines = NULL;
        Pixa* pixaTexts, *pixaImages;
        initStateVariables(env, &thiz);
        
        Pix* pixb, *pixhm;
        messageJavaCallback(MESSAGE_IMAGE_DETECTION);
        
        PixBinarizer binarizer(false);
        Pix* pixOrgClone = pixClone(pixOrg);
        pixb = binarizer.binarize(pixOrgClone, pixJavaCallback);
        pixJavaCallback(pixb);
        
//        SkewCorrector skewCorrector(false);
//        Pix* pixbRotated = skewCorrector.correctSkew(pixb, NULL);
//        pixDestroy(&pixb);
//        pixb = pixbRotated;
//        pixJavaCallback(pixb);
        
        segmentComplexLayout(pixOrg, NULL, pixb, &pixaImages, &pixaTexts, callbackLayout, true);
        
        if (isStateValid()) {
            env->CallVoidMethod(thiz, onLayoutElements, pixaTexts, pixaImages);
        }
        
        resetStateVariables();
        return (jint) 0;
    }
    
    jobject Java_com_renard_ocr_cropimage_image_1processing_Blur_nativeBlurDetect(JNIEnv *env, jobject thiz, jlong nativePix) {
        Pix *pixOrg = (PIX *) nativePix;
        PixBlurDetect blurDetector(true);
        l_float32 blurValue;
        L_TIMER timer = startTimerNested();
        Box* maxBlurLoc = NULL;
        Pix* pixBlended = blurDetector.makeBlurIndicator(pixOrg,&blurValue, &maxBlurLoc);
        l_int32 w,h,x,y;
        boxGetGeometry(maxBlurLoc,&x,&y,&w,&h);
        //pixRenderBox(pixBlended,maxBlurLoc,2,L_SET_PIXELS);
        log("pix = %p, blur=%f, box=(%i,%i - %i,%i)processing time %f\n",pixBlended, blurValue,x,y,w,h,stopTimerNested(timer));
        //create result
        jclass cls = env->FindClass("com/renard/ocr/cropimage/image_processing/BlurDetectionResult");
        jmethodID constructor = env->GetMethodID(cls, "<init>", "(JDJ)V");
        return env->NewObject(cls, constructor, (jlong)pixBlended, (jdouble)blurValue, (jlong)maxBlurLoc);
    }
    
    
    
    jlong Java_com_googlecode_tesseract_android_OCR_nativeOCRBook(JNIEnv *env, jobject thiz, jlong nativePix) {
        LOGV(__FUNCTION__);
        Pix *pixOrg = (PIX *) nativePix;
        Pix* pixText;
        initStateVariables(env, &thiz);

        bookpage(pixOrg, &pixText , messageJavaCallback, pixJavaCallback, false);

        resetStateVariables();
        
        return (jlong)pixText;
        
    }
    
#ifdef __cplusplus
}
#endif  /* __cplusplus */
