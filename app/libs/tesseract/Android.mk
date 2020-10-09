LOCAL_PATH := $(call my-dir)
TESSERACT_PATH := $(TESSERACT_PATH)/src

### jni
include $(CLEAR_VARS)
LOCAL_MODULE := libtess

LOCAL_SRC_FILES += \
  pageiterator.cpp \
  html_text.cpp \
  resultiterator.cpp \
  tessbaseapi.cpp

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)

LOCAL_LDLIBS += \
  -ljnigraphics \
  -llog

LOCAL_STATIC_LIBRARIES := libtess_static
LOCAL_SHARED_LIBRARIES := liblept

include $(BUILD_SHARED_LIBRARY)

### core static

include $(CLEAR_VARS)
LOCAL_MODULE := libtess_core_static
LOCAL_THIN_ARCHIVE := true

LOCAL_C_INCLUDES += $(LOCAL_PATH) # overriding some tesseract headers

LOCAL_PATH := $(TESSERACT_PATH)

TESSERACT_SRC_FILES := \
  $(wildcard $(TESSERACT_PATH)/arch/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/ccstruct/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/ccutil/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/classify/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/cube/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/cutil/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/dict/*.cpp) \

BLACKLIST_SRC_FILES := \
    ccutil/fileio.cpp \
    %sse.cpp \
    %avx2.cpp \
    %avx.cpp \

LOCAL_SRC_FILES := \
  $(filter-out $(BLACKLIST_SRC_FILES),$(subst $(LOCAL_PATH)/,,$(TESSERACT_SRC_FILES)))

LOCAL_C_INCLUDES += \
  $(TESSERACT_PATH)/api \
  $(TESSERACT_PATH)/arch \
  $(TESSERACT_PATH)/ccmain \
  $(TESSERACT_PATH)/ccstruct \
  $(TESSERACT_PATH)/ccutil \
  $(TESSERACT_PATH)/classify \
  $(TESSERACT_PATH)/cutil \
  $(TESSERACT_PATH)/dict \
  $(TESSERACT_PATH)/lstm \
  $(TESSERACT_PATH)/textord \
  $(TESSERACT_PATH)/training \
  $(TESSERACT_PATH)/viewer \
  $(TESSERACT_PATH)/wordrec

LOCAL_CFLAGS := \
  -DGRAPHICS_DISABLED \
  -DPACKAGE_VERSION=\"4.1.0\" \
  -DTESSERACT_MAJOR_VERSION=4 \
  -DTESSERACT_MINOR_VERSION=1 \
  -DTESSERACT_MICRO_VERSION=0 \
  -Wno-shift-negative-value

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)
LOCAL_EXPORT_CFLAGS := $(LOCAL_CFLAGS)
LOCAL_STATIC_LIBRARIES += liblept_static # to inherit C_INCLUDE and CFLAGS

include $(BUILD_STATIC_LIBRARY)

### other static (split from _core to keep linker command line less than maximum)

include $(CLEAR_VARS)
LOCAL_MODULE := libtess_static
LOCAL_THIN_ARCHIVE := true

# tesseract (minus executable)

BLACKLIST_SRC_FILES := \
  %api/tesseractmain.cpp \
  %viewer/svpaint.cpp

TESSERACT_SRC_FILES := \
  $(wildcard $(TESSERACT_PATH)/api/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/ccmain/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/lstm/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/textord/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/viewer/*.cpp) \
  $(wildcard $(TESSERACT_PATH)/wordrec/*.cpp)

LOCAL_PATH := $(TESSERACT_PATH)

LOCAL_SRC_FILES := \
  $(filter-out $(BLACKLIST_SRC_FILES),$(subst $(LOCAL_PATH)/,,$(TESSERACT_SRC_FILES)))

LOCAL_STATIC_LIBRARIES := libtess_core_static
LOCAL_STATIC_LIBRARIES += liblept_static # to inherit C_INCLUDE

include $(BUILD_STATIC_LIBRARY)
