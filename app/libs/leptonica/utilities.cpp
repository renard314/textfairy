/*
 * Copyright 2011, Google Inc.
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
#include <string.h>
#include <android/bitmap.h>
#include <cmath>

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */

/***************
 * AdaptiveMap *
 ***************/

jlong Java_com_googlecode_leptonica_android_AdaptiveMap_nativeBackgroundNormMorph(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong nativePix,
                                                                                  jint reduction,
                                                                                  jint size,
                                                                                  jint bgval) {
  // Normalizes the background of each element in pixa.

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixBackgroundNormMorph(pixs, NULL, (l_int32) reduction, (l_int32) size,
                                     (l_int32) bgval);

  return (jlong) pixd;
}

jlong Java_com_googlecode_leptonica_android_AdaptiveMap_nativePixContrastNorm(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong nativePix,
                                                                              jint sizeX,
                                                                              jint sizeY,
                                                                              jint minDiff,
                                                                              jint smoothX,
                                                                              jint smoothY) {

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixContrastNorm(NULL, pixs, (l_int32) sizeX, (l_int32) sizeY,
                                     (l_int32) minDiff, (l_int32) smoothX, (l_int32) smoothY);

  return (jlong) pixd;
}

/************
 * Binarize *
 ************/

jlong Java_com_googlecode_leptonica_android_Binarize_nativeOtsuAdaptiveThreshold(JNIEnv *env,
                                                                                 jclass clazz,
                                                                                 jlong nativePix,
                                                                                 jint sizeX,
                                                                                 jint sizeY,
                                                                                 jint smoothX,
                                                                                 jint smoothY,
                                                                                 jfloat scoreFract) {

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd;

  if (pixOtsuAdaptiveThreshold(pixs, (l_int32) sizeX, (l_int32) sizeY, (l_int32) smoothX,
                               (l_int32) smoothY, (l_float32) scoreFract, NULL, &pixd)) {
    return (jlong) 0;
  }

  return (jlong) pixd;
}

jlong Java_com_googlecode_leptonica_android_Binarize_nativeSauvolaBinarizeTiled(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong nativePix,
                                                                                jint whsize,
                                                                                jfloat factor,
                                                                                jint nx,
                                                                                jint ny) {

  PIX *pixs = (PIX *) nativePix;
  PIX *pixd;

  if (pixSauvolaBinarizeTiled(pixs, (l_int32) whsize, (l_float32) factor, (l_int32) nx,
                               (l_int32) ny, NULL, &pixd)) {
    return (jlong) 0;
  }

  return (jlong) pixd;
}

/***********
 * Convert *
 ***********/

jlong Java_com_googlecode_leptonica_android_Convert_nativeConvertTo8(JNIEnv *env, jclass clazz,
                                                                     jlong nativePix) {
  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixConvertTo8(pixs, FALSE);

  return (jlong) pixd;
}

/***********
 * Enhance *
 ***********/

jlong Java_com_googlecode_leptonica_android_Enhance_nativeUnsharpMasking(JNIEnv *env, jclass clazz,
                                                                         jlong nativePix,
                                                                         jint halfwidth,
                                                                         jfloat fract) {
  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixUnsharpMasking(pixs, (l_int32) halfwidth, (l_float32) fract);

  return (jlong) pixd;
}

/**********
 * JpegIO *
 **********/

jbyteArray Java_com_googlecode_leptonica_android_JpegIO_nativeCompressToJpeg(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong nativePix,
                                                                             jint quality,
                                                                             jboolean progressive) {
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

jlong Java_com_googlecode_leptonica_android_Scale_nativeScaleGeneral(JNIEnv *env, jclass clazz,
                                                             jlong nativePix, jfloat scaleX,
                                                             jfloat scaleY,jfloat sharpfract, jint sharpwidth) {
  LOGV("%s",__FUNCTION__);
  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixScaleGeneral(pixs, (l_float32) scaleX, (l_float32) scaleY,(l_float32) sharpfract, (l_int32) sharpwidth);
  return (jlong) pixd;
}

jlong Java_com_googlecode_leptonica_android_Scale_nativeScale(JNIEnv *env, jclass clazz,
                                                              jlong nativePix, jfloat scaleX,
                                                              jfloat scaleY) {
  PIX *pixs = (PIX *) nativePix;
  PIX *pixd = pixScale(pixs, (l_float32) scaleX, (l_float32) scaleY);

  return (jlong) pixd;
}

/********
 * Skew *
 ********/

jfloat Java_com_googlecode_leptonica_android_Skew_nativeFindSkew(JNIEnv *env, jclass clazz,
                                                                 jlong nativePix, jfloat sweepRange,
                                                                 jfloat sweepDelta,
                                                                 jint sweepReduction,
                                                                 jint searchReduction,
                                                                 jfloat searchMinDelta) {
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

jlong Java_com_googlecode_leptonica_android_Clip_nativeClipRectangle(JNIEnv *env, jclass clazz, jlong nativePix, jlong nativeBox) {
	  LOGV("%s",__FUNCTION__);

  PIX *pixs = (PIX *) nativePix;
  BOX *box = (BOX *) nativeBox;
  PIX *pixd;
  pixd = pixClipRectangle(pixs,box,NULL);
  return (jlong) pixd;
}


jlong Java_com_googlecode_leptonica_android_Clip_nativeClipRectangle2(JNIEnv *env, jclass clazz, jlong nativePix, jlong nativeBox) {
	LOGV("%s",__FUNCTION__);

	PIX *pixs = (PIX *) nativePix;
	BOX *box = (BOX *) nativeBox;
	Box* boxc = NULL;
	Pix* pixd;
	l_int32	bx, by, bw, bh, w, h, d, x, y;

	/* Clip the input box to the pix */
	pixGetDimensions(pixs, &w, &h, &d);
	if ((boxc = boxClipToRectangle(box, w, h)) == NULL) {
		L_WARNING("box doesn't overlap pix\n", __FUNCTION__);
		return 0;
	}
	boxGetGeometry(box, &x, &y, &bw, &bh);

	/* Extract the block */
	if ((pixd = pixCreate(bw, bh, d)) == NULL) {
		L_WARNING("pixd not made\n", __FUNCTION__);
		return 0;
	}

	boxGetGeometry(boxc, &bx, &by, &bw, &bh);
	pixCopyResolution(pixd, pixs);
	pixCopyColormap(pixd, pixs);

	if(x<0){
		x = std::abs(x);
	} else {
		x = 0;
	}
	if(y<0){
		y = std::abs(y);
	} else {
		y = 0;
	}

	LOGV("pixRasterop to (%i,%i) from (%i,%i); w/h = %i,%i",x,y,bx,by,bw,bh);
	//copy clip region into new pix
	pixRasterop(pixd, x, y, bw, bh, PIX_SRC, pixs, bx, by);

	boxDestroy(&boxc);
	return (jlong) pixd;
}

/**********
 * Rotate *
 **********/

jlong Java_com_googlecode_leptonica_android_Rotate_nativeRotate(JNIEnv *env, jclass clazz,
                                                                jlong nativePix, jfloat degrees,
                                                                jboolean quality, jboolean resize) {
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
    w = (resize == JNI_TRUE) ? w : 0;
    h = (resize == JNI_TRUE) ? h : 0;
    pixd = pixRotate(pixs, radians, type, L_BRING_IN_WHITE, w, h);
  }

  return (jlong) pixd;
}

jlong Java_com_googlecode_leptonica_android_Rotate_nativeRotateOrth(JNIEnv *env, jclass clazz,
                                                               jlong nativePix, jint quads) {
	LOGV("%s",__FUNCTION__);
	PIX *pixs = (PIX *) nativePix;
	PIX *pixd;
	pixd = pixRotateOrth(pixs,(int)quads);
	return (jlong) pixd;
}

/**********
 * Bilinear *
 **********/

jlong Java_com_googlecode_leptonica_android_Projective_nativeProjectivePtaGray(JNIEnv *env, jclass clazz, jlong nativePix, jfloatArray dest, jfloatArray src) {
	LOGV("%s",__FUNCTION__);

	jfloat* dest2 = env->GetFloatArrayElements( dest,0);
	jfloat* src2 = env->GetFloatArrayElements( src,0);

	PIX *pixs = (PIX *) nativePix;

	PTA* orgPoints = ptaCreate(4);
	PTA* mappedPoints = ptaCreate(4);

	ptaAddPt(orgPoints,src2[0],src2[1]);
	ptaAddPt(orgPoints,src2[2],src2[3]);
	ptaAddPt(orgPoints,src2[4],src2[5]);
	ptaAddPt(orgPoints,src2[6],src2[7]);

	ptaAddPt(mappedPoints, dest2[0], dest2[1]);
	ptaAddPt(mappedPoints, dest2[2], dest2[3]);
	ptaAddPt(mappedPoints, dest2[4], dest2[5]);
	ptaAddPt(mappedPoints, dest2[6], dest2[7]);
	LOGI("src points: (%.1f,%.1f) - (%.1f,%.1f) - (%.1f,%.1f) - (%.1f,%.1f)",src2[0],src2[1],src2[2],src2[3],src2[4],src2[5],src2[6],src2[7]);
	LOGI("dest points: (%.1f,%.1f) - (%.1f,%.1f) - (%.1f,%.1f) - (%.1f,%.1f)",dest2[0],dest2[1],dest2[2],dest2[3],dest2[4],dest2[5],dest2[6],dest2[7]);

	Pix* pixBilinar = pixProjectivePtaGray(pixs,mappedPoints,orgPoints,0xff);

	ptaDestroy(&orgPoints);
	ptaDestroy(&mappedPoints);
	env->ReleaseFloatArrayElements(dest, dest2, 0);
	env->ReleaseFloatArrayElements(src, src2, 0);

	return (jlong) pixBilinar;
}


#ifdef __cplusplus
}
#endif  /* __cplusplus */
