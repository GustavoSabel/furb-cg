package br.furb.bte.controle.ia;

import java.util.Arrays;
import java.util.Collection;
import br.furb.bte.Tela;
import br.furb.bte.comando.ProcessadorComando;
import br.furb.bte.comando.ProcessadorPadrao;
import br.furb.bte.controle.Controlador;
import br.furb.bte.controle.input.CenarioInput;
import br.furb.bte.controle.input.GameplayInput;
import br.furb.bte.controle.input.KeyboardInput;
import br.furb.bte.controle.input.Moto2Input;
import br.furb.bte.ia.MyTronBot;
import br.furb.bte.objetos.Arena;
import br.furb.bte.objetos.Moto;

public class ControladorIA extends Controlador {

    private Tela tela;

    private ProcessadorPadrao<Tela> processadorGameplay;
    private ProcessadorPadrao<Tela> processadorCenario;
    private ProcessadorComando<Moto> processadorMotos;
    private final CenarioInput cenarioInput;
    private final GameplayInput gameplayInput;
    private final Moto2Input motoInput;
    private Direcao direcaoAtual;

    private final Collection<KeyboardInput<?>> allInputs;

    public ControladorIA() {
	criarProcessadores();
	cenarioInput = new CenarioInput(processadorCenario);
	gameplayInput = new GameplayInput(processadorGameplay);
	motoInput = new Moto2Input(null, processadorMotos);

	allInputs = Arrays.asList(cenarioInput, gameplayInput, motoInput);
    }

    private void criarProcessadores() {
	processadorCenario = new ProcessadorPadrao<>();
	processadorGameplay = new ProcessadorPadrao<>();
	processadorMotos = new ProcessadorPadrao<>();
    }

    @Override
    public void associarTela(Tela tela) {
	if (this.tela != null) {
	    allInputs.forEach(i -> i.desassociarTela());
	}
	this.tela = tela;
	processadorCenario.setTela(tela);
	processadorGameplay.setTela(tela);
	if (tela != null) {
	    allInputs.forEach(i -> i.associarTela(tela));
	    tela.addGameplayListener(this);
	}
    }

    @Override
    public void onReset() {
	if (tela != null) {
	    Arena arena = tela.getArena();
	    motoInput.associarMoto(arena.getMoto((short) 2));
	}
    }

    @Override
    public void beforePlay() {
	if (tela != null) {
	    String mapa = tela.getMapa();
	    Direcao move = toDirecao(MyTronBot.processMove(mapa));

	    Lado lado = movimentoParaGirarPara(move);

	    if (lado != null) {
		if (lado == Lado.Direita) {
		    //		    motoInput.tratarTecla(new KeyD);
		} else {
		    //		    motoInput.tratarTecla(new KeyS);
		}
		this.direcaoAtual = move;
	    }

	}
    }

    /**
     * Retorna o movimento necessário para girar para a nova direção
     * 
     * @param direcaoDestino
     * @return
     */
    private Lado movimentoParaGirarPara(Direcao direcaoDestino) {
	switch (this.direcaoAtual) {
	    case Norte:
		switch (direcaoDestino) {
		    case Leste:
			return Lado.Direita;
		    case Oeste:
			return Lado.Esquerda;
		}
		break;
	    case Sul:
		switch (direcaoDestino) {
		    case Leste:
			return Lado.Esquerda;
		    case Oeste:
			return Lado.Direita;
		}
		break;
	    case Leste:
		switch (direcaoDestino) {
		    case Norte:
			return Lado.Esquerda;
		    case Oeste:
			return Lado.Direita;
		}
		break;
	    case Oeste:
		switch (direcaoDestino) {
		    case Norte:
			return Lado.Direita;
		    case Oeste:
			return Lado.Esquerda;
		}
		break;
	}
	System.out.println("Direção inválida. Impossível girar de " + this.direcaoAtual.name() + " para "
		+ direcaoDestino.name());
	return null;
    }

    private Direcao toDirecao(String processMove) {
	char move = processMove.toUpperCase().charAt(0);
	switch (move) {
	    case 'N':
		return Direcao.Norte;
	    case 'S':
		return Direcao.Sul;
	    case 'E':
		return Direcao.Leste;
	    case 'W':
		return Direcao.Oeste;
	}
	System.out.println("Direção inválida: " + processMove);
	return null;
    }
}
