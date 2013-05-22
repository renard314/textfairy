################################################################################
# Automatically-generated file. Do not edit!
################################################################################

-include ../makefile.init

RM := rm

# All of the sources participating in the build are defined here
-include sources.mk
-include src/utility/subdir.mk
-include src/objdir/utility/subdir.mk
-include src/objdir/lib/subdir.mk
-include src/objdir/frontends/subdir.mk
-include src/objdir/codecs/subdir.mk
-include src/objdir/bardecode/subdir.mk
-include src/lib/subdir.mk
-include src/frontends/subdir.mk
-include src/codecs/subdir.mk
-include Debug/src/utility/subdir.mk
-include Debug/src/lib/subdir.mk
-include Debug/src/frontends/subdir.mk
-include Debug/src/codecs/subdir.mk
-include subdir.mk
-include objects.mk

ifneq ($(MAKECMDGOALS),clean)
ifneq ($(strip $(C++_DEPS)),)
-include $(C++_DEPS)
endif
ifneq ($(strip $(C_DEPS)),)
-include $(C_DEPS)
endif
ifneq ($(strip $(CC_DEPS)),)
-include $(CC_DEPS)
endif
ifneq ($(strip $(CPP_DEPS)),)
-include $(CPP_DEPS)
endif
ifneq ($(strip $(CXX_DEPS)),)
-include $(CXX_DEPS)
endif
ifneq ($(strip $(C_UPPER_DEPS)),)
-include $(C_UPPER_DEPS)
endif
endif

-include ../makefile.defs

# Add inputs and outputs from these tool invocations to the build variables 

# All Target
all: hocr2pdf

# Tool invocations
hocr2pdf: $(OBJS) $(USER_OBJS)
	@echo 'Building target: $@'
	@echo 'Invoking: MacOS X C++ Linker'
	g++ -L/usr/local/bin -o "hocr2pdf" $(OBJS) $(USER_OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '

# Other Targets
clean:
	-$(RM) $(C++_DEPS)$(OBJS)$(C_DEPS)$(CC_DEPS)$(CPP_DEPS)$(EXECUTABLES)$(CXX_DEPS)$(C_UPPER_DEPS) hocr2pdf
	-@echo ' '

.PHONY: all clean dependents
.SECONDARY:

-include ../makefile.targets
