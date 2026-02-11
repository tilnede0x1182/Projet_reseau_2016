compile:
	@mkdir -p build
	javac -d build src/*.java

run:
	java -cp build Main

compile_run: compile run

javadoc:
	@mkdir -p javadoc
	javadoc -d javadoc src/*.java

clean:
	rm -rf build javadoc
