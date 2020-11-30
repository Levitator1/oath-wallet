package com.levitator.oath_wallet_service.ui.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/*
* I could be wrong, but I think it's insane to fully reimplement this interface
* every single time it needs to be specialized, so here is a base class that does
* nothing by default, and you override the methods you actually need.
*/
public class MouseListenerBase implements MouseListener{

    @Override
    public void mouseClicked(MouseEvent me) {        
    }

    @Override
    public void mousePressed(MouseEvent me) {        
    }

    @Override
    public void mouseReleased(MouseEvent me) {
    }

    @Override
    public void mouseEntered(MouseEvent me) {        
    }

    @Override
    public void mouseExited(MouseEvent me) {        
    }
    
}
