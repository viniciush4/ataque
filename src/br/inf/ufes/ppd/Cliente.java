package br.inf.ufes.ppd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cliente 
{
	private static byte[] lerArquivoCriptografado(String nomeArquivo, byte[] palavraConhecida, int tamanhoVetorGerado) throws IOException
	{
		File arquivo = new File(nomeArquivo);
		byte[] mensagem = null;
		
		//Verifica se o arquivo não existe
		if(!arquivo.exists())
		{
			try
			{
				//Cria um vetor de bytes aleatório
		        mensagem = new byte[tamanhoVetorGerado];
		        new Random().nextBytes(mensagem);
		        
		        //Criptografa o vetor de bytes aleatório
		        byte[] key = palavraConhecida;
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
	
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.ENCRYPT_MODE, keySpec);
	
				mensagem = cipher.doFinal(mensagem);
		        
		        //Salva o vetor em um arquivo
				salvarArquivo(nomeArquivo, mensagem);
			}
			catch (Exception e) 
			{	
				e.printStackTrace();
			}			
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
			throw new Exception("Falta um parâmetro, verifique se o IP do Mestre foi fornecido.");
		}
		else if(args.length < 2) 
		{
			throw new Exception("Falta um parâmetro, verifique se um nome para o arquivo foi fornecido.");
		}
		else if(args.length < 3)
		{
			throw new Exception("Falta um parâmetro, verifique se uma palavra conhecida foi fornecida.");
		}
		
		//Captura os parâmetros
		String ipMestre = args[0];
		String nomeArquivoCriptografado = args[1];
		byte[] palavraConhecida = args[2].getBytes();
		int tamanhoVetorGerado = (args.length < 4) ? (1000 + numeroAleatorio.nextInt(99001)) : Integer.parseInt(args[3]);
		
		try
		{
			//Armazena o vetor de bytes
			byte[] mensagem = lerArquivoCriptografado(nomeArquivoCriptografado, palavraConhecida, tamanhoVetorGerado);
			
			//Invoca o mestre passando o vetor de bytes e a palavra conhecida
			Registry registry = LocateRegistry.getRegistry(ipMestre);
		
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
