package br.furb.bte.comando;

public enum TipoComando {

    DIREITA,
    ESQUERDA,
    COLISAO,
    CONFIG_MOTO,
    READY,
    PAUSE,
    RESUME,
    STEP,
    RESET,
    TOGGLE_PERSPECTIVE,
    // ===== COMANDOS REMOTOS =====
    ESTADO_MOTO,
    HELLO,
    BYE;

    public short getCodigo() {
	return (short) ordinal();
    }

}
