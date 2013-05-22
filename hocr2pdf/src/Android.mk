LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := hocr2pdf

LOCAL_LDLIBS := \
  -lz \
  -llog
  
  
LOCAL_CPP_EXTENSION :=.cc

LOCAL_SRC_FILES := \
  lib/Image.cc \
  lib/crop.cc \
  lib/scale.cc \
  lib/rotate.cc \
  lib/Colorspace.cc \
  lib/hocr.cc \
  codecs/Codecs.cc \
  codecs/pdf.cc \
  codecs/jpeg.cc \
  codecs/transupp.c

LOCAL_C_INCLUDES := \
  codecs \
  utility \
  lib \
  $(LIBJPEG_PATH)
  

#common
LOCAL_SHARED_LIBRARIES:= libjpeg
LOCAL_PRELINK_MODULE:= false

include $(BUILD_SHARED_LIBRARY)

