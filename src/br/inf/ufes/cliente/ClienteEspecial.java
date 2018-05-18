package br.inf.ufes.cliente;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;

public class ClienteEspecial 
{
	private static byte[] palavraConhecida;
	private static List<String> dicionario = new ArrayList<String>();
	
	private static void lerDicionario()
	{
		try 
		{
			FileReader arq = new FileReader("../dictionary.txt");
			BufferedReader lerArq = new BufferedReader(arq);
 
			// lê a primeira linha
			// a variável "linha" recebe o valor "null" quando o processo
			// de repetição atingir o final do arquivo texto
			String linha = lerArq.readLine(); 
			
			// Adiciona a palavra no dicionario
			dicionario.add(linha);
			
			while (linha != null) 
			{
				linha = lerArq.readLine(); // lê da segunda até a última linha
				
				if(linha != null)
				{
					dicionario.add(linha);
				}
			}
 
			arq.close();
		} 
		catch (IOException e) 
		{
			System.err.printf("Erro na abertura do arquivo: %s.\n",e.getMessage());
		}
	}

	private static void setPalavraConhecida(byte[] palavra)
	{
		palavraConhecida = palavra;
	}
	
	private static byte[] gerarMensagem(int tamanhoVetorGerado)
	{
		byte[] mensagem = null;
		Random numeroAleatorio = new Random();
		
		try
		{
			//Cria um vetor de bytes aleatório
			mensagem = new byte[tamanhoVetorGerado];
	        new Random().nextBytes(mensagem);
	        
	        //Armazena um trecho conhecido do vetor de bytes aleatório
	        //A faixa pode ser alterada
	        byte[] palavra = Arrays.copyOfRange(mensagem, 0,5);
	        
	        //Salva o trecho conhecido
	        setPalavraConhecida(palavra);
	        
	        //Criptografa o vetor de bytes aleatório usando uma palavra aleatória do dicionário
	        byte[] key = dicionario.get(numeroAleatorio.nextInt(dicionario.size())).getBytes();
	        
//	        System.out.println(dicionario.get(numeroAleatorio.nextInt(dicionario.size()))+"\n"+key);
	        
			SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
	
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
	
			mensagem = cipher.doFinal(mensagem);
			
		}
		catch (Exception e) 
		{	
			System.err.printf("Erro no método gerarMensagem: %s.\n",e.getMessage());
		}	
		
		return mensagem;
	}
	
	public static void main(String[] args) throws Exception 
	{	
		// Se os argumentos não foram fornecidos
		if(args.length < 3)
		{
			throw new Exception("Uso: ClienteEspecial <IP_DO_MESTRE> <NÚMERO_DE_ATAQUES> <TAMANHO_VETOR_INICIAL> <INTERVALO_VETORES>");
		}
		
		//Captura os parâmetros
		String ipMestre = args[0];
		int numeroDeAtaques = Integer.parseInt(args[1]);
		int tamanhoVetorGerado = Integer.parseInt(args[2]); 
		int intervaloVetor = Integer.parseInt(args[3]);
		
		Cronometro cronometro = new Cronometro();
		byte[] mensagem;
		Guess[] guess;
		
		try
		{	
			//Invoca o mestre passando o vetor de bytes e a palavra conhecida
			Registry registry = LocateRegistry.getRegistry(ipMestre);
		
			Master mestre = (Master) registry.lookup("mestre");
			
			//Cria o arquivo para salvar os tempos
			PrintStream write = new PrintStream("Tempos.csv");
			write.print("Tamanho do Vetor,Horas,Minutos,Segundos,Milesimos\n");
			
			//Armazena as palavras do dicionário
			lerDicionario();
			
			//Chama os ataques de acordo com o número de ataques passado na linha de comando
			for(int i=0; i<numeroDeAtaques; i++)
			{
				//Gera um arquivo aleatório
				mensagem = gerarMensagem(tamanhoVetorGerado);
		
				//Tempo inicial
				cronometro.start();
				
				//Chama um ataque
				guess = mestre.attack(mensagem, palavraConhecida);
				
				//Tempo final
				cronometro.stop();
				
				System.out.println("Voltou!");
				
				//Salvar o tempo
				write.print(tamanhoVetorGerado+","+cronometro+"\n");
				
				//Zera o cronometro
				cronometro.zerar();
				
				//Aumenta o tamanho do vetor
				tamanhoVetorGerado += intervaloVetor;
			}
			
			
		    write.close();
		}
		catch (Exception e) 
		{
			System.err.printf("Erro no método main: %s.\n",e.getMessage());
		}
	}
}
