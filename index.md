## Ataque de Dicionário em Mensagem Criptografada

O trabalho consiste em implementar a arquitetura mestre/escravo para realizar um ataque de dicionário em uma mensagem criptografada. O ataque de dicionário consiste em usar um dicionário como fonte de chaves candidatas. No nosso caso, assumiremos que conhecemos um trecho da mensagem (por exemplo sabemos que a string “ufes” aparece na mensagem, ou a string “PDF”). O ataque será realizado decriptografando a mensagem com cada palavra do dicionário (chave candidata) e procurando o trecho conhecido na mensagem decriptografada. Caso o trecho conhecido seja localizado na mensagem decriptografada com a chave candidata, a chave é considerada uma possível chave para a mensagem, e é usada para obter um possível texto para a mensagem.

O algoritmo de criptografia Blowfish será utilizado (http://www.schneier.com/blowfish-
download.html).
