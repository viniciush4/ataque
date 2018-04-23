package br.inf.ufes.ppd;

import java.util.Random;

public class Cliente 
{
	public static void main(String[] args) 
	{
		Random numeroAleatorio = new Random();
		
		String nomeArquivoCriptografado = (args.length < 1) ? null : args[0];
		String palavraConhecida = (args.length < 2) ? null : args[1];
		int tamanhoVetorGerado = (args.length < 3) ?  (1000 + numeroAleatorio.nextInt(99000)) : Integer.parseInt(args[2]);
		


		System.out.println(tamanhoVetorGerado);
	}

}
