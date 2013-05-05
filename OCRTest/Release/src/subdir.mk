################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/binarize.cpp \
../src/experiments.cpp \
../src/main.cpp \
../src/pageseg.cpp \
../src/util.cpp 

OBJS += \
./src/binarize.o \
./src/experiments.o \
./src/main.o \
./src/pageseg.o \
./src/util.o 

CPP_DEPS += \
./src/binarize.d \
./src/experiments.d \
./src/main.d \
./src/pageseg.d \
./src/util.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/usr/local/include/tesseract -I/usr/local/include/leptonica -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


