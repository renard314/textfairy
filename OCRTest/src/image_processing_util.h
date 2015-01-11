/*  This file is part of Text Fairy.
 
 Text Fairy is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Text Fairy is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Text Fairy.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * util.h
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 */

#ifndef UTIL_H_
#define UTIL_H_

#include "image_processing.h"
#include <cstdio>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "image processing (native)"
#define log(fmt,args...)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, fmt, ##args)
#endif
#ifndef __ANDROID__
#include <cstdio>
#define log(fmt,args...)  ({std::printf(fmt, ##args);std::printf("\n");fflush(stdout);})
#endif


void combineSelectedPixa(Pixa* pixaText, Pixa* pixaImage, l_int32* textindexes, l_int32 textCount, l_int32* imageindexes, l_int32 imageCount,void (*callbackMessage) (const int), Pix** pPixFinal, Pix** pPixOcr, Boxa** pBoxaColumns, bool debug);
void bookpage(Pix* pixOrg, Pix** pixFinal, void(*messageJavaCallback) (int), void(*pixJavaCallback) (Pix*) ,bool debug);

#endif /* UTIL_H_ */
