################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CC_SRCS += \
../src/lib/Colorspace.cc \
../src/lib/Image.cc \
../src/lib/crop.cc \
../src/lib/hocr.cc \
../src/lib/rotate.cc \
../src/lib/scale.cc 

OBJS += \
./src/lib/Colorspace.o \
./src/lib/Image.o \
./src/lib/crop.o \
./src/lib/hocr.o \
./src/lib/rotate.o \
./src/lib/scale.o 

CC_DEPS += \
./src/lib/Colorspace.d \
./src/lib/Image.d \
./src/lib/crop.d \
./src/lib/hocr.d \
./src/lib/rotate.d \
./src/lib/scale.d 


# Each subdirectory must supply rules for building sources it contributes
src/lib/%.o: ../src/lib/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


