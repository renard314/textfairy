ANDROID_JPEG_NO_ASSEMBLER := true
APP_PLATFORM := android-9
APP_STL := gnustl_static #gnustl_shared
# ARMv7 is significanly faster due to the use of the hardware FPU
APP_ABI := armeabi armeabi-v7a x86 
APP_OPTIM := release
#APP_CPPFLAGS += -frtti
