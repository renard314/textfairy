/*
 * Copyright 2010, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "common.h"
#include "string.h"
#include "android/bitmap.h"

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */
 
jint Java_com_googlecode_leptonica_android_Dewarp_nativeDewarp(JNIEnv *env, jclass clazz, jint nativePix) {
	  LOGV("%s",__FUNCTION__);
	  PIX* pixd=NULL;
  PIX *pixs = (PIX *) nativePix;
  PIX* pixb = pixThresholdToBinary(pixs, 130);
  //  Basic functioning:
  L_Dewarp *dew = dewarpCreate(pixb,0,60,5,1);
  int buildresult=dewarpBuildModel(dew, 0);
  if(buildresult==0){
    int applyresult=dewarpApplyDisparity(dew, pixs, 0);
    if(applyresult==0){
      pixd = pixClone(dew->pixd);
    }
  }
  pixDestroy(&pixb);
  dewarpDestroy(&dew);
  
  if(pixd!=NULL){
    return (jint) pixd;
  } else {
    return (jint) 0;
  }
}


jint Java_com_googlecode_leptonica_android_Morphology_nativeTophat(JNIEnv *env,
                                                                                 jclass clazz,
                                                                                 jint nativePix) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  if(pixs==0){
	  LOGI("pixs is 0");

  }


  PIX* pixsg = pixConvertRGBToLuminance(pixs);
  LOGI("luminance");
  if(pixsg==0){
	  LOGI("lumi error");

  }
  /* Black tophat (closing - original-image) and invert */
  PIX* pixg = pixTophat(pixsg, 15, 15, L_TOPHAT_BLACK);
  LOGI("tophat");
  if(pixg==0){
	  LOGI("tophat error");
  }
  pixInvert(pixg, pixg);
  LOGI("invert");
  if(pixg==0){
	  LOGI("invert error");
  }
 /* Set black point at 200, white point at 245. */
  PIX* pixd = pixGammaTRC(NULL, pixg, 1.0, 200, 245);
  LOGI("gamma");
  if(pixd==0){
	  LOGI("gamma error");

  }

  pixDestroy(&pixsg);
  pixDestroy(&pixg);

  return (jint) pixd;
}

/***************
 * AdaptiveMap *
 ***************/

jint Java_com_googlecode_leptonica_android_AdaptiveMap_nativeBackgroundNormMorph(JNIEnv *env,
                                                                                 jclass clazz,
                                                                                 jint nativePix,
                                                                                 jint reduction,
                                                                                 jint size,
                                                                                 jint bgval) {
	  LOGV("%s",__FUNCTION__);
// Normalizes the background of each element in pixa.

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixBackgroundNormMorph(pixs, NULL, (l_int32) reduction, (l_int32) size,
                                     (l_int32) bgval);

  return (jint) pixd;
}

/************
 * Binarize *
 ************/

jint Java_com_googlecode_leptonica_android_Binarize_nativeOtsuAdaptiveThreshold(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jint nativePix,
                                                                                jint sizeX,
                                                                                jint sizeY,
                                                                                jint smoothX,
                                                                                jint smoothY,
                                                                                jfloat scoreFract) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd;

  if (pixOtsuAdaptiveThreshold(pixs, (l_int32) sizeX, (l_int32) sizeY, (l_int32) smoothX,
                               (l_int32) smoothY, (l_float32) scoreFract, NULL, &pixd)) {
    return (jint) 0;
  }

  return (jint) pixd;
}

/***********
 * Convert *
 ***********/

jint Java_com_googlecode_leptonica_android_Convert_nativeConvertTo8(JNIEnv *env, jclass clazz,
                                                                    jint nativePix) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixConvertTo8(pixs, FALSE);

  return (jint) pixd;
}

/***********
 * Enhance *
 ***********/

jint Java_com_googlecode_leptonica_android_Enhance_nativeUnsharpMasking(JNIEnv *env, jclass clazz,
                                                                        jint nativePix,
                                                                        jint halfwidth,
                                                                        jfloat fract) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixUnsharpMasking(pixs, (l_int32) halfwidth, (l_float32) fract);

  return (jint) pixd;
}

/**********
 * JpegIO *
 **********/

jbyteArray Java_com_googlecode_leptonica_android_JpegIO_nativeCompressToJpeg(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jint nativePix,
                                                                             jint quality,
                                                                             jboolean progressive) {
	  LOGV("%s",__FUNCTION__);

  PIX *pix = (PIX *) nativePix;

  l_uint8 *data;
  size_t size;

  if (pixWriteMemJpeg(&data, &size, pix, (l_int32) quality, progressive == JNI_TRUE ? 1 : 0)) {
    LOGE("Failed to write JPEG data");

    return NULL;
  }

  // TODO Can we just use the byte array directly?
  jbyteArray array = env->NewByteArray(size);
  env->SetByteArrayRegion(array, 0, size, (jbyte *) data);

  free(data);

  return array;
}

/*********
 * Scale *
 *********/

jint Java_com_googlecode_leptonica_android_Scale_nativeScaleGeneral(JNIEnv *env, jclass clazz,
                                                             jint nativePix, jfloat scaleX,
                                                             jfloat scaleY,jfloat sharpfract, jint sharpwidth) {
  LOGV("%s",__FUNCTION__);
  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixScaleGeneral(pixs, (l_float32) scaleX, (l_float32) scaleY,(l_float32) sharpfract, (l_int32) sharpwidth);
  return (jint) pixd;
}


jint Java_com_googlecode_leptonica_android_Scale_nativeScale(JNIEnv *env, jclass clazz,
                                                             jint nativePix, jfloat scaleX,
                                                             jfloat scaleY) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixScale(pixs, (l_float32) scaleX, (l_float32) scaleY);

  return (jint) pixd;
}

/********
 * Skew *
 ********/

jfloat Java_com_googlecode_leptonica_android_Skew_nativeFindSkew(JNIEnv *env, jclass clazz,
                                                                 jint nativePix, jfloat sweepRange,
                                                                 jfloat sweepDelta,
                                                                 jint sweepReduction,
                                                                 jint searchReduction,
                                                                 jfloat searchMinDelta) {
	  LOGV("%s",__FUNCTION__);
// Corrects the rotation of each element in pixa to 0 degrees.

  PIX *pixs = (PIX *) nativePix;

  l_float32 angle, conf;

  if (!pixFindSkewSweepAndSearch(pixs, &angle, &conf, (l_int32) sweepReduction,
                                 (l_int32) searchReduction, (l_float32) sweepRange,
                                 (l_int32) sweepDelta, (l_float32) searchMinDelta)) {
    if (conf <= 0) {
      return (jfloat) 0;
    }

    return (jfloat) angle;
  }

  return (jfloat) 0;
}


/**********
 * Rotate *
 **********/

jint Java_com_googlecode_leptonica_android_Rotate_nativeCleanbackground(JNIEnv *env, jclass clazz,
                                                               jint nativePix) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixt1,*pixt2,*pixt3,*pixd;
  pixt1 = pixCloseGray (pixs, 11, 11);
  pixd = pixBlockconv(pixt1, 15, 15);
//  pixt3 = pixConvertTo8(pixt2, FALSE);


//  pixd = pixSubtract(NULL,pixs,pixt3);
  pixDestroy(&pixt1);
//  pixDestroy(&pixt2);
//  pixDestroy(&pixt3);
  return (jint) pixd;
}

jint Java_com_googlecode_leptonica_android_Rotate_nativeRotateOrth(JNIEnv *env, jclass clazz,
                                                               jint nativePix, jint quads) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd;
  pixd = pixRotateOrth(pixs,(int)quads);
  return (jint) pixd;
}

jint Java_com_googlecode_leptonica_android_Clip_nativeClipRectangle(JNIEnv *env, jclass clazz, jint nativePix, jint nativeBox) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  BOX *box = (BOX *) nativeBox;
  PIX *pixd;
  pixd = pixClipRectangle(pixs,box,NULL);
  return (jint) pixd;
}


jint Java_com_googlecode_leptonica_android_Rotate_nativeRotate(JNIEnv *env, jclass clazz,
                                                               jint nativePix, jfloat degrees,
                                                               jboolean quality) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixd;
  PIX *pixs = (PIX *) nativePix;

  l_float32 deg2rad = 3.1415926535 / 180.0;
  l_float32 radians = degrees * deg2rad;
  l_int32 w, h, bpp, type;

  pixGetDimensions(pixs, &w, &h, &bpp);

  if (bpp == 1 && quality == JNI_TRUE) {
    pixd = pixRotateBinaryNice(pixs, radians, L_BRING_IN_WHITE);
  } else {
    type = quality == JNI_TRUE ? L_ROTATE_AREA_MAP : L_ROTATE_SAMPLING;
    pixd = pixRotate(pixs, radians, type, L_BRING_IN_WHITE, 0, 0);
  }

  return (jint) pixd;
}


#ifdef __cplusplus
}
#endif  /* __cplusplus */
