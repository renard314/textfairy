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
#include <string.h>
#include <jni.h>

#include <iostream>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cmath>
#include <cctype>
#include <vector>

#include "Codecs.hh"
#include "pdf.hh"
#include "hocr.hh"
#include "jpeg.hh"
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

int hocr2pdf(const char* imageFileName, const char* hocrText, PDFCodec* pdfContext, bool sloppy, bool overlayImage) {
	  LOGI("hocr2pdf %s",hocrText);

	// load the image, if specified and possible

	  Image image; image.w = image.h = 0;
	  std::string fileName(imageFileName);
	  if (!ImageCodec::Read(fileName, image)) {
		  LOGI("Error reading input file.");
          overlayImage = false;
        std::string hocrString(hocrText);
        std::size_t startPos = hocrString.find("bbox", 0);
        if (startPos!=std::string::npos) {
            std::size_t endPos = hocrString.find(";", startPos);
            if (endPos!=std::string::npos) {
                std::string subString =  hocrString.substr(startPos, endPos-startPos);
                LOGI("size string = %s",subString.c_str());
                std::string buf;
                std::stringstream ss(subString);
                std::vector<std::string> tokens;

                while (ss >> buf) {
                    tokens.push_back(buf);
                }
                std::stringstream(tokens[3]) >> image.w;
                std::stringstream(tokens[4]) >> image.h;

                LOGI("image size = %i, %i, ", image.w, image.h);

            }
        }
	  }

	  if (image.resolutionX() <= 0 || image.resolutionY() <= 0) {
	    LOGI("Warning: Image x/y resolution not set, defaulting to: 300 ");
	    image.setResolution(300, 300);
	  }

	  unsigned int res = image.resolutionX();

	  std::stringstream hocr(hocrText);
    
      LOGI("paged dimensions %.2f, %.2f", 72. * image.w / res, 72. * image.h / res);


	  pdfContext->beginPage(72. * image.w / res, 72. * image.h / res);
	  pdfContext->setFillColor(0, 0, 0);
	  hocr2pdf(hocr, pdfContext, res, sloppy,!overlayImage);

	  if (overlayImage) {
        LOGI("Overlaying image");
	    pdfContext->showImage(image, 0, 0, 72. * image.w / res, 72. * image.h / res);
	  }

	  return 0;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv *env;

  if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
    LOGE("Failed to get the environment using GetEnv()");
    return -1;
  }

  return JNI_VERSION_1_6;
}


void Java_com_renard_ocr_pdf_Hocr2Pdf_nativeHocr2pdf( JNIEnv* env, jobject thiz, jobjectArray imageStrings, jobjectArray hocrBytes, jstring out, jboolean sloppy, jboolean overlayImage)
{
  LOGI("Java_com_renard_pdf_Hocr2Pdf_nativeHocr2pdf");

  bool c_sloppy = (sloppy == JNI_TRUE);
  bool c_overlay = (overlayImage==JNI_TRUE);
  const char *pdfFileName = env->GetStringUTFChars(out, NULL);
  bool useCallbacks = true;

  jclass cls = env->GetObjectClass(thiz);
  jmethodID mid = env->GetMethodID(cls, "onProgress", "(I)V");
  if (mid == 0) {
	  useCallbacks = false;
  }

  std::ofstream pdfOutStream(pdfFileName);
  PDFCodec* pdfContext = new PDFCodec(&pdfOutStream);

  unsigned int numImages = env->GetArrayLength( imageStrings );
  for (int i = 0; i< numImages; i++) {
	  jstring image = (jstring) env->GetObjectArrayElement( imageStrings, i );
	  jbyteArray hocr = (jbyteArray)env->GetObjectArrayElement( hocrBytes, i );

	  const char *c_image = env->GetStringUTFChars(image, NULL);
	  jbyte* javaStringByte = env->GetByteArrayElements(hocr,0);
	  jsize javaStringlen = env->GetArrayLength(hocr);
	  std::string cString((char*)javaStringByte, javaStringlen);

	  //const char *c_hocr = env->GetStringUTFChars(hocr, NULL);

	  LOGI("size of string = %i",cString.length());
	  if (useCallbacks) {
		  env->CallVoidMethod(thiz, mid, i);
	  }

	  int r = hocr2pdf(c_image, cString.c_str(), pdfContext, c_sloppy,c_overlay);
	  env->ReleaseStringUTFChars(image, c_image);
	  env->ReleaseByteArrayElements(hocr,javaStringByte,JNI_ABORT);
	  env->DeleteLocalRef(hocr);
	  env->DeleteLocalRef(image);
  }
  delete pdfContext;

  env->ReleaseStringUTFChars(out, pdfFileName);
}
#ifdef __cplusplus
}
#endif

