#include <string.h>
#include <jni.h>

#include <iostream>
#include <fstream>
#include <sstream>
#include <fstream>
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
	  // load the image, if specified and possible

	  Image image; image.w = image.h = 0;
	  std::string fileName(imageFileName);
	  if (!ImageCodec::Read(fileName, image)) {
		  LOGI("Error reading input file.");
		  return 1;
	  }

	  if (image.resolutionX() <= 0 || image.resolutionY() <= 0) {
	    LOGI("Warning: Image x/y resolution not set, defaulting to: 300 ");
	    image.setResolution(300, 300);
	  }

	  unsigned int res = image.resolutionX();

	  std::stringstream hocr(hocrText);


	  pdfContext->beginPage(72. * image.w / res, 72. * image.h / res);
	  pdfContext->setFillColor(0, 0, 0);
	  hocr2pdf(hocr, pdfContext, res, sloppy,!overlayImage);

	  if (overlayImage) {
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


void Java_com_renard_pdf_Hocr2Pdf_nativeHocr2pdf( JNIEnv* env, jobject thiz, jobjectArray imageStrings, jobjectArray hocrStrings, jstring out, jboolean sloppy, jboolean overlayImage)
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
	  jstring hocr = (jstring)env->GetObjectArrayElement( hocrStrings, i );

	  const char *c_image = env->GetStringUTFChars(image, NULL);
	  const char *c_hocr = env->GetStringUTFChars(hocr, NULL);

	  if (useCallbacks) {
		  env->CallVoidMethod(thiz, mid, i);
	  }

	  int r = hocr2pdf(c_image, c_hocr, pdfContext, c_sloppy,c_overlay);

	  env->ReleaseStringUTFChars(image, c_image);
	  env->ReleaseStringUTFChars(hocr, c_hocr);
  }
  delete pdfContext;

  env->ReleaseStringUTFChars(out, pdfFileName);
}
#ifdef __cplusplus
}
#endif

