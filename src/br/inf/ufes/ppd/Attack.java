package br.inf.ufes.ppd;

import java.util.ArrayList;

public class Attack {
	
	private int attackNumber;
	private int quantidadeSubataquesEmAndamento;
	ArrayList<Guess> guesses = new ArrayList<Guess>();
	
	public Attack(int attackNumber) {
		this.attackNumber = attackNumber;
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}

	public int getQuantidadeSubataquesEmAndamento() {
		return quantidadeSubataquesEmAndamento;
	}

	public void incrementaSubataquesEmAndamento() {
		this.quantidadeSubataquesEmAndamento++;
	}
	
	public void decrementaSubataquesEmAndamento() throws Exception {
		this.quantidadeSubataquesEmAndamento--;
		
		if(this.quantidadeSubataquesEmAndamento == 0) {
			throw new Exception();
		}
	}
}
