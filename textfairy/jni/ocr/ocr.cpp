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
#include "baseapi.h"
#include "ocrclass.h"
#include <pthread.h>
#include <cmath>
#include "binarize.h"
#include "pageseg.h"
#include "resultiterator.h"
#include <util.h>
#include "text_search.h"

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */

static jmethodID onFinalPix, onProgressImage, onImageSize, onProgressValues, onProgressText, onHOCRResult, onLayoutElements, onUTF8Result, onLayoutPix;

static JavaVM *g_jvm = NULL;
static Box* currentTextBox = NULL;
l_int32 lastProgress = 0;
static bool cancel_ocr = false;

static JNIEnv *cachedEnv;
static jobject* cachedObject;
Pix* cachedTextLines;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {

	g_jvm = vm;

	return JNI_VERSION_1_6;
}

void Java_com_googlecode_tesseract_android_OCR_nativeInit(JNIEnv *env, jobject _thiz) {
	jclass cls;
	cls = env->FindClass("com/googlecode/tesseract/android/OCR");
	onFinalPix = env->GetMethodID(cls, "onFinalPix", "(I)V");
	onProgressImage = env->GetMethodID(cls, "onProgressImage", "(I)V");
	onProgressValues = env->GetMethodID(cls, "onProgressValues", "(IIIIIIIII)V");
	onProgressText = env->GetMethodID(cls, "onProgressText", "(I)V");
	onHOCRResult = env->GetMethodID(cls, "onHOCRResult", "(Ljava/lang/String;I)V");
	onLayoutElements = env->GetMethodID(cls, "onLayoutElements", "(II)V");
	onUTF8Result = env->GetMethodID(cls, "onUTF8Result", "(Ljava/lang/String;)V");
	onLayoutPix = env->GetMethodID(cls, "onLayoutPix", "(I)V");
	onImageSize = env->GetMethodID(cls, "onImageSize", "(II)V");

}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
	g_jvm = NULL;
}

void initStateVariables(JNIEnv* env, jobject *object) {
	cancel_ocr = false;
	cachedEnv = env;
	cachedObject = object;
	lastProgress = 0;
}

void resetStateVariables() {
	cancel_ocr = false;
	cachedEnv = NULL;
	cachedObject = NULL;
	lastProgress = 0;
}

bool isStateValid() {

	if (cancel_ocr == false && cachedEnv != NULL && cachedObject != NULL) {
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

void pixJavaCallback(Pix* pix, bool debug = FALSE, bool scale = TRUE) {
	if (isStateValid()) {
		L_TIMER timer;
		l_int32 depth = pixGetDepth(pix);
		if (debug) {
			timer = startTimerNested();
		}
		Pix* pixpreview = NULL;
		if (scale == TRUE) {
			if (depth == 1) {
				pixpreview = pixReduceBinary2(pix, NULL);
			} else {
				pixpreview = pixScale(pix, 0.25, 0.25);
			}
		} else {
			pixpreview = pixClone(pix);
		}
		if (isStateValid()) {
			cachedEnv->CallVoidMethod(*cachedObject, onProgressImage, (jint) pixpreview);
		}
		if (debug) {
			ostringstream debugstring;
			debugstring << "showPix: " << stopTimerNested(timer);
			LOGI(debugstring.str().c_str());
		}
	}
}
bool cancelFunc(void* cancel_this, int words) {
	return cancel_ocr;
}

bool progressJavaCallback(int progress, int left, int right, int top, int bottom) {

	if (isStateValid() && currentTextBox != NULL) {
		if (progress > lastProgress || left != 0 || right != 0 || top != 0 || bottom != 0) {
			int x, y, w, h;
			boxGetGeometry(currentTextBox, &x, &y, &w, &h);
			cachedEnv->CallVoidMethod(*cachedObject, onProgressValues, progress, (jint) left, (jint) right, (jint) top, (jint) bottom, (jint) x, (jint) (x + w), (jint) y, (jint) (y + h));
			lastProgress = progress;
		}
	}
	return true;
}


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

	/*do ocr on text parts*/
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

jint Java_com_googlecode_tesseract_android_OCR_nativeOCR(JNIEnv *env, jobject thiz, jint nativePixaText, jint nativePixaImage, jintArray selectedTexts, jintArray selectedImages, jstring tessDir, jstring lang, jboolean useCube) {
	LOGV(__FUNCTION__);
	Pixa *pixaTexts = (PIXA *) nativePixaText;
	Pixa *pixaImages = (PIXA *) nativePixaImage;
	ostringstream hocr;
	ostringstream utf8text;
	initStateVariables(env, &thiz);

	jint* textindexes = env->GetIntArrayElements(selectedTexts, 0);
	jsize textCount = env->GetArrayLength(selectedTexts);
	jint* imageindexes = env->GetIntArrayElements(selectedImages, 0);
	jsize imageCount = env->GetArrayLength(selectedImages);

	const char *tessDirNative = env->GetStringUTFChars(tessDir, 0);
	const char *langNative = env->GetStringUTFChars(lang, 0);

	lastProgress = 0;

	Pix* pixFinal;
	Pix* pixOcr;
	Boxa* boxaColumns;

	combineSelectedPixa(pixaTexts, pixaImages, textindexes, textCount, imageindexes, imageCount, messageJavaCallback, &pixFinal, &pixOcr, &boxaColumns, true);

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
}

void callbackLayout(const Pix* pixpreview) {
	if (isStateValid()) {
		cachedEnv->CallVoidMethod(*cachedObject, onLayoutPix, pixpreview);
	}
	messageJavaCallback(MESSAGE_ANALYSE_LAYOUT);
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
	pixJavaCallback(pixsg, false, true);
	binarize(pixsg, pixhm, &pixb);
	pixDestroy(&pixsg);


	segmentComplexLayout(pixOrg, pixhm, pixb, &pixaImages, &pixaTexts, callbackLayout, true);

	if (isStateValid()) {
		env->CallVoidMethod(thiz, onLayoutElements, pixaTexts, pixaImages);
	}

	resetStateVariables();
	return (jint) 0;
}

jint Java_com_googlecode_tesseract_android_OCR_nativeCancelOCR(JNIEnv *env, jobject _thiz) {
	LOGV(__FUNCTION__);
	cachedEnv = NULL;
	cachedObject = NULL;
	if (cancel_ocr == true) {
		return 0;
	} else {
		cancel_ocr = true;
		return 1;
	}
}

jint Java_com_googlecode_tesseract_android_OCR_nativeOCRBook(JNIEnv *env, jobject thiz, jint nativePix, jstring tessDir, jstring lang) {
	LOGV(__FUNCTION__);
	Pix *pixOrg = (PIX *) nativePix;
	Pix* pixFinal;
	Pix* pixText;
	ostringstream hocr;
	ostringstream utf8text;
	//TODO callback to java with size of pix with border

	const char *tessDirNative = env->GetStringUTFChars(tessDir, 0);
	const char *langNative = env->GetStringUTFChars(lang, 0);

	LOGI(tessDirNative);
	LOGI(langNative);

	initStateVariables(env, &thiz);

	pixText = bookpage(pixOrg, &pixFinal, messageJavaCallback, pixJavaCallback,true, true);
	pixDestroy(&pixOrg);

	int w = pixGetWidth(pixText);
	int h = pixGetHeight(pixText);

    if (isStateValid()) {
        cachedEnv->CallVoidMethod(*cachedObject, onImageSize, w,h);
    }


	currentTextBox = boxCreate(0, 0, w, h);

	messageJavaCallback(MESSAGE_OCR);

	int accuracy = doOCR(pixText, &hocr, &utf8text, tessDirNative, langNative, true);

	pixDestroy (&pixText);
	boxDestroy(&currentTextBox);

	env->ReleaseStringUTFChars(tessDir, tessDirNative);
	env->ReleaseStringUTFChars(lang, langNative);

	if (isStateValid()) {
		jstring result = env->NewStringUTF(hocr.str().c_str());
		env->CallVoidMethod(thiz, onHOCRResult, result,accuracy);
	}

	if (isStateValid()) {
		jstring result = env->NewStringUTF(utf8text.str().c_str());
		env->CallVoidMethod(thiz, onUTF8Result, result);
	}

	if (isStateValid()) {
		LOGI("calling  onFinalPix");
		env->CallVoidMethod(thiz, onFinalPix, pixFinal);
	}
	hocr.str("");
	utf8text.str("");

	resetStateVariables();
	return (jint) 0;

}

#ifdef __cplusplus
}
#endif  /* __cplusplus */
