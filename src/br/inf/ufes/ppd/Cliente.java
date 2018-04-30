package br.inf.ufes.ppd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class Cliente 
{
	private static byte[] lerArquivoCriptografado(String nomeArquivo, int tamanhoVetorGerado) throws IOException
	{
		File arquivo = new File(nomeArquivo);
		byte[] mensagem;
		
		//Verifica se o arquivo não existe
		if(!arquivo.exists())
		{
			//Cria um vetor de bytes aleatório
	        mensagem = new byte[tamanhoVetorGerado];
	        new Random().nextBytes(mensagem);
	        //SecureRandom.getInstanceStrong().nextBytes(mensagem);
	        
	        //Salva o vetor em um arquivo
			salvarArquivo(nomeArquivo, mensagem);
		}
		else
		{	
		    InputStream is = new FileInputStream(arquivo);
	        long length = arquivo.length();
	        
	        // creates array (assumes file length<Integer.MAX_VALUE)
	        mensagem = new byte[(int)length];
	        
	        int offset = 0; 
	        int count = 0;
	        
	        while ((offset < mensagem.length) && (count = is.read(mensagem, offset, mensagem.length-offset)) >= 0) 
	        {
	            offset += count;
	        }
	        
	        is.close();
		}
        
        return mensagem;
	}

	private static void salvarArquivo(String nomeArquivo, byte[] mensagem) throws IOException
	{
		FileOutputStream out = new FileOutputStream(nomeArquivo);
	    out.write(mensagem);
	    out.close();
	}
	
	public static void main(String[] args) throws Exception 
	{
		Random numeroAleatorio = new Random();
		
		// Se os argumentos não foram fornecidos
		if(args.length < 1) 
		{
			throw new Exception("Informe um nome para o arquivo.");
		}
		else if(args.length < 2)
		{
			throw new Exception("Informe uma palavra conhecida.");
		}
		
		//Captura os parâmetros
		String nomeArquivoCriptografado = args[0];
		byte[] palavraConhecida = args[1].getBytes();
		int tamanhoVetorGerado = (args.length < 3) ? (1000 + numeroAleatorio.nextInt(99001)) : Integer.parseInt(args[2]);
		
		try
		{
			//Armazena o vetor de bytes
			byte[] mensagem = lerArquivoCriptografado(nomeArquivoCriptografado,tamanhoVetorGerado);
			
			//Invoca o mestre passando o vetor de bytes e a palavra conhecida
			Registry registry = LocateRegistry.getRegistry();
		
			Master mestre = (Master) registry.lookup("mestre");
			
			Guess[] guess = mestre.attack(mensagem, palavraConhecida);
			
			//Imprime as chaves candidatas e salva as mensagens candidatas em arquivos
			for(Guess g: guess)
			{
				System.out.println("Chave candidata encontrada: " + g.getKey());
				salvarArquivo(g.getKey()+".msg", g.getMessage());
			}
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
