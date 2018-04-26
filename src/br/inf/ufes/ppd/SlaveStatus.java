package br.inf.ufes.ppd;

public class SlaveStatus {

	private String nome;
	
	public SlaveStatus(String slaveName) {
		this.nome = slaveName;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}
}
