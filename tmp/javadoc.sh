#!/bin/bash

javadoc -d javadoc *.java -private

# -private sert à ne pas traiter uniquement les classes public et protected, 
# comme c'est traité par défaut.
