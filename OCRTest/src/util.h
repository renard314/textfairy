/*
 * util.h
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 */

#ifndef UTIL_H_
#define UTIL_H_

#include "image_processing.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "Text Fairy (native)"
#define log(fmt,args...)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, fmt, ##args)
#endif
#ifndef __ANDROID__
#include <cstdio>
#define log(fmt,args...)  ({std::printf(fmt, ##args);std::printf("\n");fflush(stdout);})
#endif

void combineSelectedPixa(Pixa* pixaText, Pixa* pixaImage, l_int32* textindexes, l_int32 textCount, l_int32* imageindexes, l_int32 imageCount,void (*callbackMessage) (const int), Pix** pPixFinal, Pix** pPixOcr, Boxa** pBoxaColumns, bool debug);
Pix* bookpage(Pix* pixOrg, Pix** pixFinal, void(*messageJavaCallback) (int), void(*pixJavaCallback) (Pix*,bool,bool) ,bool showIntermediate, bool debug);

#endif /* UTIL_H_ */
