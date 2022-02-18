# note: must be run from the project root folder

# compile
@echo "Compiling."
mvn clean package assembly:single
@echo "Finished compiling."