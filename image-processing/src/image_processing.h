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
 * common.h
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 */

#ifndef COMMON_H_
#define COMMON_H_
#include <iostream>
#include "allheaders.h"
#include <sstream>
#include <cmath>

#ifdef __ANDROID_API__
#include <android/log.h>
#define LOG_TAG "ImageProcessing"
#define printf(fmt,args...)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, fmt, ##args)
/*dont write debug images onto the sd card*/
#define pixWrite(name,pixs,format)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, name)
#endif



static const int MESSAGE_IMAGE_DETECTION = 0;
static const int MESSAGE_IMAGE_DEWARP = 1;
static const int MESSAGE_OCR = 2;
static const int MESSAGE_ASSEMBLE_PIX = 3;
static const int MESSAGE_ANALYSE_LAYOUT = 4;

#define MAX_INT16 0x7fff



#endif /* COMMON_H_ */
