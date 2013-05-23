################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/hocr2pdf.cpp 

CC_SRCS += \
../src/ArgumentList.cc 

OBJS += \
./src/ArgumentList.o \
./src/hocr2pdf.o 

CC_DEPS += \
./src/ArgumentList.d 

CPP_DEPS += \
./src/hocr2pdf.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I"/home/renard/devel/workspace/helloCPP/src/codecs" -I"/home/renard/devel/workspace/helloCPP/src/lib" -I"/home/renard/devel/workspace/helloCPP/src" -I"/home/renard/devel/workspace/helloCPP/src/utility" -I/home/renard/devel/jpeg-8c -I/home/renard/devel/zlib-1.2.5 -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

src/%.o: ../src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I"/home/renard/devel/workspace/helloCPP/src/codecs" -I"/home/renard/devel/workspace/helloCPP/src/lib" -I"/home/renard/devel/workspace/helloCPP/src" -I"/home/renard/devel/workspace/helloCPP/src/utility" -I/home/renard/devel/jpeg-8c -I/home/renard/devel/zlib-1.2.5 -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


