cmake_minimum_required(VERSION 3.13)

project(cnmcu)

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
find_package(JNI REQUIRED)

file(TO_CMAKE_PATH "${GENERATED_SOURCES_DIR}" GENERATED_SOURCES_DIR)

file(GLOB GENERATED_SOURCES "${GENERATED_SOURCES_DIR}/*.cpp")

set(PROJECT_SOURCES 
  cnmcu/mos6502.cpp
  cnmcu/Nano.cpp
  cnmcu/CNUART.cpp
  cnmcu.cpp

  ${GENERATED_SOURCES}
)

set(PROJECT_INCLUDES
  cnmcu
  ${JNI_INCLUDE_DIRS}
  ${GENERATED_SOURCES_DIR}
)

add_library(${PROJECT_NAME} SHARED ${PROJECT_SOURCES})

set_target_properties(${PROJECT_NAME} PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
    INTERPROCEDURAL_OPTIMIZATION ON
    CXX_VISIBILITY_PRESET "hidden"
    VISIBILITY_INLINES_HIDDEN ON
)

target_include_directories(${PROJECT_NAME} PRIVATE ${PROJECT_INCLUDES})

target_link_libraries(${PROJECT_NAME} ${PROJECT_LIBRARIES})

# force off-tree build
if(${CMAKE_SOURCE_DIR} STREQUAL ${CMAKE_BINARY_DIR})
  message(FATAL_ERROR "CMake generation is not allowed within the source directory!
  Remove the CMakeCache.txt file and try again from another folder, e.g.:
    mkdir build && cd build
    cmake ..
  ")
endif()

# default to Release build
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING
      "Choose the type of build, options are: None Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif()

# Determine target cpu architecture and set TARGET_ARCH

string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" SYSTEM_PROCESSOR)

if(SYSTEM_PROCESSOR MATCHES "64" OR SYSTEM_PROCESSOR MATCHES "armv8")
  set(IS_64BIT ON)
else()
  set(IS_64BIT OFF)
endif()

if(SYSTEM_PROCESSOR MATCHES "arm" OR SYSTEM_PROCESSOR MATCHES "aarch")
  if(IS_64BIT)
    set(TARGET_ARCH "arm64")
  else()
    set(TARGET_ARCH "arm32")
  endif()
else()
  if(IS_64BIT)
    set(TARGET_ARCH "x64")
  else()
    set(TARGET_ARCH "x86")
  endif()
endif()

install(TARGETS ${PROJECT_NAME} RUNTIME DESTINATION "${TARGET_ARCH}"
    LIBRARY DESTINATION "${TARGET_ARCH}"
)
