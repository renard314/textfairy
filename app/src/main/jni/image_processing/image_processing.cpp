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
#include "binarize.h"
#include "pageseg.h"
#include "PixBlurDetect.h"
#include <image_processing_util.h>

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */

static jmethodID onProgressImage, onProgressValues, onProgressText, onHOCRResult, onLayoutElements, onUTF8Result, onLayoutPix;

static JNIEnv *cachedEnv;
static jobject* cachedObject;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	return JNI_VERSION_1_6;
}

void Java_com_googlecode_tesseract_android_OCR_nativeInit(JNIEnv *env, jobject _thiz) {
	jclass cls;
	cls = env->FindClass("com/googlecode/tesseract/android/OCR");
	onProgressImage = env->GetMethodID(cls, "onProgressImage", "(J)V");
	onProgressText = env->GetMethodID(cls, "onProgressText", "(I)V");
	onHOCRResult = env->GetMethodID(cls, "onHOCRResult", "(Ljava/lang/String;I)V");
	onLayoutElements = env->GetMethodID(cls, "onLayoutElements", "(II)V");
	onUTF8Result = env->GetMethodID(cls, "onUTF8Result", "(Ljava/lang/String;)V");
	onLayoutPix = env->GetMethodID(cls, "onLayoutPix", "(J)V");

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
		Pix* pixpreview = pixClone(pix);
		Pix* pixScaled = pixScaleByIntSampling(pixpreview,0.25);
		cachedEnv->CallVoidMethod(*cachedObject, onProgressImage, (jlong) pixScaled);
		pixDestroy(&pixpreview);
		pixDestroy(&pixScaled);
	}
}

void callbackLayout(const Pix* pixpreview) {
	if (isStateValid()) {
		cachedEnv->CallVoidMethod(*cachedObject, onLayoutPix, (jlong)pixpreview);
	}
	messageJavaCallback(MESSAGE_ANALYSE_LAYOUT);
}

/*

int doOCR(Pix* pixb, ostringstream* hocr, ostringstream* utf8,  const char* const tessDir, const char* const lang,  bool debug = false) {
	ETEXT_DESC monitor;

	monitor.progress_callback = progressJavaCallback;
	monitor.cancel = cancelFunc;

	tesseract::TessBaseAPI api;
	LOGI("OCR LANG = %s",lang);
	api.Init(tessDir, lang, tesseract::OEM_DEFAULT);

	api.SetPageSegMode(tesseract::PSM_AUTO);

	if (debug) {
		startTimer();
	}

	api.SetImage(pixb);
	LOGI("ocr start");
	const char* hocrtext = api.GetHOCRText(&monitor, 0);
	LOGI("ocr finished");
	int accuracy = 0;
	if (hocrtext != NULL && isStateValid()) {
		*hocr << hocrtext;
		tesseract::ResultIterator* it = api.GetIterator();
		LOGI("start GetHTMLText");
		std::string utf8text = GetHTMLText(it, 70);

		//std::string utf8text = api.GetUTF8Text();
		LOGI("after GetHTMLText");
		if (!utf8text.empty()) {
			*utf8 << utf8text;
		}
        accuracy = api.MeanTextConf();
		if (debug) {
			ostringstream debugstring;
			debugstring << "ocr: " << stopTimer() << std::endl << "confidence: " << api.MeanTextConf() << std::endl;
			LOGI(debugstring.str().c_str());
		}
	} else {
		LOGI("ocr was cancelled");
		if (debug) {
			ostringstream debugstring;
			debugstring << "ocr: " << stopTimer() << std::endl << "ocr was cancelled" << std::endl;
			LOGI(debugstring.str().c_str());
		}
	}
	if (hocrtext != NULL) {
		delete[] hocrtext;
	}
	api.End();
    return accuracy;
}

int doMultiOcr(Pix* pixOCR, Boxa* boxaColumns, ostringstream* hocrtext, ostringstream* utf8text, const char* const tessDir, const char* const lang, const bool debug, const bool usecube) {
	l_int32 xb, yb, wb, hb;
	l_int32 columnCount = boxaGetCount(boxaColumns);

	//do ocr on text parts
	tesseract::TessBaseAPI api;
	ETEXT_DESC monitor;
	monitor.progress_callback = progressJavaCallback;
	monitor.cancel = cancelFunc;
	api.Init(tessDir, lang, tesseract::OEM_DEFAULT);
	api.SetPageSegMode(tesseract::PSM_SINGLE_BLOCK);
	api.SetImage(pixOCR);

	messageJavaCallback(MESSAGE_OCR);
	float accuracy = 0;

	for (int i = 0; i < columnCount; i++) {
		if (boxaGetBoxGeometry(boxaColumns, i, &xb, &yb, &wb, &hb)) {
			continue;
		}
		currentTextBox = boxCreate(xb, yb, wb, hb);
		api.SetRectangle(xb, yb, wb, hb);
		LOGI("start OCR");
		const char* hocr = api.GetHOCRText(&monitor, 0);
		if (hocr != NULL && isStateValid()) {
			*hocrtext << hocr;
			delete[] hocr;
			tesseract::ResultIterator* it = api.GetIterator();
			std::string utf8 = GetHTMLText(it, 70);
			if (!utf8.empty()) {
				*utf8text << utf8;
			}
			accuracy += api.MeanTextConf();
		} else {
			boxDestroy(&currentTextBox);
			break;
		}

		boxDestroy(&currentTextBox);
	}
	api.End();
	return accuracy/columnCount;
}

*/

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
/*
	const char *tessDirNative = env->GetStringUTFChars(tessDir, 0);
	const char *langNative = env->GetStringUTFChars(lang, 0);

	pixJavaCallback(pixFinal, TRUE, TRUE);
	LOGI("after showPixRGB");

	if (isStateValid()) {
		cachedEnv->CallVoidMethod(*cachedObject, onFinalPix, pixFinal);
		LOGI("after CallVoidMethod");
	}

	cancel_ocr = false;

	int accuracy = doMultiOcr(pixOcr, boxaColumns, &hocr, &utf8text, tessDirNative, langNative, true, useCube);

	pixDestroy(&pixOcr);
	boxaDestroy(&boxaColumns);

	env->ReleaseStringUTFChars(tessDir, tessDirNative);
	env->ReleaseStringUTFChars(lang, langNative);

	if (isStateValid()) {
		jstring result = env->NewStringUTF(hocr.str().c_str());
		env->CallVoidMethod(thiz, onHOCRResult, result, accuracy);
	}
	if (isStateValid()) {
		jstring result = env->NewStringUTF(utf8text.str().c_str());
		env->CallVoidMethod(thiz, onUTF8Result, result);
	}

	env->ReleaseIntArrayElements(selectedTexts, textindexes, 0);
	env->ReleaseIntArrayElements(selectedImages, imageindexes, 0);
	utf8text.str("");
	hocr.str("");
	resetStateVariables();

	return (jint) 0;
*/

}



jint Java_com_googlecode_tesseract_android_OCR_nativeAnalyseLayout(JNIEnv *env, jobject thiz, jint nativePix) {
	LOGV(__FUNCTION__);
	Pix *pixOrg = (PIX *) nativePix;
	Pix* pixTextlines = NULL;
	Pixa* pixaTexts, *pixaImages;
	initStateVariables(env, &thiz);

	Pix* pixb, *pixhm;
	messageJavaCallback(MESSAGE_IMAGE_DETECTION);

	Pix* pixsg;
	extractImages(pixOrg, &pixhm, &pixsg);
	pixJavaCallback(pixsg);
	binarize(pixsg, pixhm, &pixb);
	pixDestroy(&pixsg);


	segmentComplexLayout(pixOrg, pixhm, pixb, &pixaImages, &pixaTexts, callbackLayout, true);

	if (isStateValid()) {
		env->CallVoidMethod(thiz, onLayoutElements, pixaTexts, pixaImages);
	}

	resetStateVariables();
	return (jint) 0;
}

jobject Java_com_renard_image_1processing_Blur_nativeBlurDetect(JNIEnv *env, jobject thiz, jlong nativePix) {
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
	jclass cls = env->FindClass("com/renard/image_processing/BlurDetectionResult");
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
