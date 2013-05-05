################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CC_SRCS += \
../src/utility/tests/Attribute.cc \
../src/utility/tests/Delete.cc \
../src/utility/tests/File.cc \
../src/utility/tests/Find.cc \
../src/utility/tests/Logging.cc \
../src/utility/tests/Thread.cc \
../src/utility/tests/ThreadedFind.cc \
../src/utility/tests/pstream.cc 

OBJS += \
./src/utility/tests/Attribute.o \
./src/utility/tests/Delete.o \
./src/utility/tests/File.o \
./src/utility/tests/Find.o \
./src/utility/tests/Logging.o \
./src/utility/tests/Thread.o \
./src/utility/tests/ThreadedFind.o \
./src/utility/tests/pstream.o 

CC_DEPS += \
./src/utility/tests/Attribute.d \
./src/utility/tests/Delete.d \
./src/utility/tests/File.d \
./src/utility/tests/Find.d \
./src/utility/tests/Logging.d \
./src/utility/tests/Thread.d \
./src/utility/tests/ThreadedFind.d \
./src/utility/tests/pstream.d 


# Each subdirectory must supply rules for building sources it contributes
src/utility/tests/%.o: ../src/utility/tests/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I"/home/renard/devel/workspace/helloCPP/src/codecs" -I/home/renard/devel/agg-2.5/include -I/usr/lib/sigc++-2.0/include -I/usr/include/sigc++-2.0 -I"/home/renard/devel/workspace/helloCPP/src/lib" -I"/home/renard/devel/workspace/helloCPP/src" -I"/home/renard/devel/workspace/helloCPP/src/utility" -I/home/renard/devel/jpeg-8c -I/home/renard/devel/zlib-1.2.5 -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


