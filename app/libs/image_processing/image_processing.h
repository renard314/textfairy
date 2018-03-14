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

#ifndef IMAGE_PROCESSING_H
#define IMAGE_PROCESSING_H

#include <jni.h>
#include <android/log.h>
#include <assert.h>
#include <unistd.h>
#include <iostream>
#include <allheaders.h>
#include <cstring>

#define IMAGE_PROCESSING_LOG_TAG "Text Fairy (native)"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)
#define LOG_ASSERT(_cond, ...) if (!_cond) __android_log_assert("conditional", IMAGE_PROCESSING_LOG_TAG, __VA_ARGS__)

#define printf(fmt,args...)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, fmt, ##args)
/*dont write debug images onto the sd card*/
#define pixWrite(name,pixs,format)  __android_log_print(ANDROID_LOG_INFO  ,LOG_TAG, name)

#endif
