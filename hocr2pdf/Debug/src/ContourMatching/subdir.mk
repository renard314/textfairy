################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CC_SRCS += \
../src/ContourMatching/ContourMatching.cc \
../src/ContourMatching/Contours.cc \
../src/ContourMatching/distance.cc \
../src/ContourMatching/segmentation.cc 

OBJS += \
./src/ContourMatching/ContourMatching.o \
./src/ContourMatching/Contours.o \
./src/ContourMatching/distance.o \
./src/ContourMatching/segmentation.o 

CC_DEPS += \
./src/ContourMatching/ContourMatching.d \
./src/ContourMatching/Contours.d \
./src/ContourMatching/distance.d \
./src/ContourMatching/segmentation.d 


# Each subdirectory must supply rules for building sources it contributes
src/ContourMatching/%.o: ../src/ContourMatching/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I"/home/renard/devel/workspace/helloCPP/src/codecs" -I/home/renard/devel/agg-2.5/include -I/usr/lib/sigc++-2.0/include -I/usr/include/sigc++-2.0 -I"/home/renard/devel/workspace/helloCPP/src/lib" -I"/home/renard/devel/workspace/helloCPP/src" -I"/home/renard/devel/workspace/helloCPP/src/utility" -I/home/renard/devel/jpeg-8c -I/home/renard/devel/zlib-1.2.5 -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


