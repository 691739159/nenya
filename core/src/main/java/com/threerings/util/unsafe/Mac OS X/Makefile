#
# $Id: Makefile 3332 2005-02-03 01:30:33Z mdb $

#
# Executable definitions

CC=gcc
RM=rm
CP=cp
MKDIR=mkdir

#
# Directory definitions

ROOT=../../../../../../../..
LIBRARIES_PATH=
OSINCDIR!=uname -s | tr 'A-Z' 'a-z'
INSTALL_PATH=${ROOT}/dist/lib/`uname -m`-Darwin

#
# Parameter and file definitions

INCLUDES=-I.. -I/System/Library/Frameworks/JavaVM.framework/Headers
LIBRARIES=
TARGET=libunsafe.jnilib

#
# Target definitions

all: ${TARGET}

install:
	@${MKDIR} -p ${INSTALL_PATH}
	cp ${TARGET} ${INSTALL_PATH}

${TARGET}: com_threerings_util_unsafe_Unsafe.c
	@echo "Compiling $<"
	@${CC} ${INCLUDES} -c com_threerings_util_unsafe_Unsafe.c \
	    -o com_threerings_util_unsafe_Unsafe.o
	@echo "Creating $@"
	@${CC} -o ${TARGET} com_threerings_util_unsafe_Unsafe.o \
	    ${LIBRARIES} ${LIBRARIES_PATH} -dynamiclib

clean:
	-${RM} -f *.o ${TARGET}
