################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/codecs/transupp.c 

CC_SRCS += \
../src/codecs/Codecs.cc \
../src/codecs/jpeg.cc \
../src/codecs/pdf.cc 

OBJS += \
./src/codecs/Codecs.o \
./src/codecs/jpeg.o \
./src/codecs/pdf.o \
./src/codecs/transupp.o 

C_DEPS += \
./src/codecs/transupp.d 

CC_DEPS += \
./src/codecs/Codecs.d \
./src/codecs/jpeg.d \
./src/codecs/pdf.d 


# Each subdirectory must supply rules for building sources it contributes
src/codecs/%.o: ../src/codecs/%.cc
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

src/codecs/%.o: ../src/codecs/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


