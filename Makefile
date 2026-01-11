compile:
	@mkdir -p build
	javac -d build src/*.java

run: compile
	java -cp build Main

javadoc:
	@mkdir -p javadoc
	javadoc -d javadoc src/*.java

clean:
	rm -rf build javadoc
