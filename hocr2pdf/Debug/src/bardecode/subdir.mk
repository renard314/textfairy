################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CC_SRCS += \
../src/bardecode/Scanner.cc 

OBJS += \
./src/bardecode/Scanner.o 

CC_DEPS += \
./src/bardecode/Scanner.d 


# Each subdirectory must supply rules for building sources it contributes
src/bardecode/%.o: ../src/bardecode/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I"/home/renard/devel/workspace/helloCPP/src/codecs" -I/home/renard/devel/agg-2.5/include -I/usr/lib/sigc++-2.0/include -I/usr/include/sigc++-2.0 -I"/home/renard/devel/workspace/helloCPP/src/lib" -I"/home/renard/devel/workspace/helloCPP/src" -I"/home/renard/devel/workspace/helloCPP/src/utility" -I/home/renard/devel/jpeg-8c -I/home/renard/devel/zlib-1.2.5 -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


