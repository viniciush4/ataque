package br.inf.ufes.cliente;

public class Cronometro 
{
	private long horaInicial = 0;
    private long horaFinal = 0;

    public Cronometro() 
    {
        super();
    }

    public void start() 
    {
        horaInicial = System.currentTimeMillis();
        horaFinal = horaInicial;
    }

    public void stop() 
    {
        horaFinal = System.currentTimeMillis();
    }

    public void zerar()
    {
    	horaInicial = 0;
    	horaFinal = 0;
    }
    
    public long elapsedTime()
    {
        return horaFinal - horaInicial;
    }

    public String toString() 
    {
        final long FATOR_SEGUNDO = 1000;
        final long FATOR_MINUTO = FATOR_SEGUNDO * 60;
        final long FATOR_HORA = FATOR_MINUTO * 60;

        long tempo = elapsedTime();
        long horas, minutos, segundos, milesimos;

        horas = tempo / FATOR_HORA;
        minutos = (tempo % FATOR_HORA) / FATOR_MINUTO;
        segundos = (tempo % FATOR_MINUTO) / FATOR_SEGUNDO;
        milesimos = tempo % FATOR_SEGUNDO;

        //return horas + " horas, " + minutos + " minutos, " + segundos + " segundos e " + milesimos + " milesimos";
        return horas+","+minutos+","+segundos+","+milesimos;
    }
}
