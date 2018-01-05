FROM mock-api
# Adapted from https://hub.docker.com/r/bitriseio/docker-android/~/dockerfile/ (MIT license)


### Android Specific Dependencies


ENV ANDROID_HOME /android-sdk

RUN apt-get update -qq

# C Libs Required by Android tools
RUN dpkg --add-architecture i386
RUN apt-get update -qq
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-8-jdk libc6:i386 libstdc++6:i386 libgcc1:i386 libncurses5:i386 libz1:i386


# Download SDK tools
RUN cd /opt \
    && wget -q https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip -O android-sdk-tools.zip \
    && unzip -q android-sdk-tools.zip -d ${ANDROID_HOME} \
    && rm android-sdk-tools.zip

ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

# Install platform tools
RUN yes | sdkmanager --licenses
RUN sdkmanager "emulator" "tools" "platform-tools"
RUN yes | sdkmanager "platforms;android-27"
RUN yes | sdkmanager "platforms;android-26"
RUN yes | sdkmanager "platforms;android-26"
RUN yes | sdkmanager "build-tools;27.0.0"
RUN yes | sdkmanager "build-tools;26.0.2"
RUN yes | sdkmanager "system-images;android-25;google_apis;armeabi-v7a"
RUN yes | sdkmanager "extras;android;m2repository"
RUN yes | sdkmanager "extras;google;m2repository"
RUN yes | sdkmanager "extras;google;google_play_services"

RUN apt-get clean


RUN DEBIAN_FRONTEND=noninteractive apt-get install -y libqt5widgets5
ENV QT_QPA_PLATFORM offscreen
ENV LD_LIBRARY_PATH ${ANDROID_HOME}/tools/lib64:${ANDROID_HOME}/emulator/lib64:${ANDROID_HOME}/emulator/lib64/qt/lib



### Execution of script

RUN echo "no" | avdmanager create avd --name "NexusEmulator" -k "system-images;android-25;google_apis;armeabi-v7a"

# Copy the current directory contents into the container at /hugsnag
ADD . /opt/hugsnag-android

# Copy notifier specific script
COPY android.sh /opt/android.sh

CMD ["./hugsnag.sh", "android"]
