# Install openJDK version 21 (includes maven, gradle, and node)
FROM cimg/openjdk:21.0.2-node

# set user to root to allow apt-get to run
USER root

ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID

# Create non-root user vscode with sudo support
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    #
    # Create a non-root user to use if preferred - see https://aka.ms/vscode-remote/containers/non-root-user.
    && groupadd --gid $USER_GID $USERNAME \
    && useradd -s /bin/bash --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME\
    && chmod 0440 /etc/sudoers.d/$USERNAME

# [Optional] Uncomment this line to install global node packages.
# RUN npm install -g <your-package-list-here>

# install kafkacat for testing purposes
RUN apt-get update && apt-get install -y kafkacat

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>