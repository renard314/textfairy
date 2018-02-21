LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libhocr2pdf

LOCAL_CPP_EXTENSION :=.cc

# jni

LOCAL_SRC_FILES += \
  hocr2pdf.cc \
  hocr2pdf/src/lib/Image.cc \
  hocr2pdf/src/lib/crop.cc \
  hocr2pdf/src/lib/scale.cc \
  hocr2pdf/src/lib/rotate.cc \
  hocr2pdf/src/lib/Colorspace.cc \
  hocr2pdf/src/lib/hocr.cc \
  hocr2pdf/src/lib/entities.cc \
  hocr2pdf/src/codecs/Codecs.cc \
  hocr2pdf/src/codecs/pdf.cc \
  hocr2pdf/src/codecs/png.cc \
  hocr2pdf/src/codecs/jpeg.cc \
  hocr2pdf/src/codecs/transupp.c

LOCAL_C_INCLUDES += \
  $(HOCR2PDF_PATH)/src/codecs \
  $(HOCR2PDF_PATH)/src/utility \
  $(HOCR2PDF_PATH)/src/lib


LOCAL_LDLIBS := \
  -llog \
  -lz

#common
LOCAL_SHARED_LIBRARIES:= libjpeg libpngo
LOCAL_DISABLE_FORMAT_STRING_CHECKS:=true

include $(BUILD_SHARED_LIBRARY)