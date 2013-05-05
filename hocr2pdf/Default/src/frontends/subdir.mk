################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CC_SRCS += \
../src/frontends/hocr2pdfTest.cc 

OBJS += \
./src/frontends/hocr2pdfTest.o 

CC_DEPS += \
./src/frontends/hocr2pdfTest.d 


# Each subdirectory must supply rules for building sources it contributes
src/frontends/%.o: ../src/frontends/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -O2 -g -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


