<?php

/*
 | Çdo fjalë që merr klienti.
 |
 | Tri rregulla vlejnë për këtë skedar dhe për çdo përkthim të tij:
 |
 |  1. Asgjë promocionale. Pa oferta, pa çmime, pa zbritje, pa "nxitoni". Një kujtesë për
 |     një pajisje të caktuar është mesazh shërbimi; reklama nuk është.
 |  2. Mos lër të kuptohet kurrë se garancia rrezikohet, përveçse kur është e vërtetë.
 |     Garancia ligjore e blerësit nuk humbet nga një servis i munguar.
 |  3. Çdo mesazh thotë nga kush vjen dhe si ndalohet.
 */

return [

    'card' => <<<'TXT'
    :installer

    :type juaj është regjistruar: :unit
    Seria: :serial
    Instaluar: :installed
    Garancia deri: :warranty

    Ruajeni këtë link. Është prova juaj se çfarë u instalua dhe kur, dhe prej andej kërkoni servisin:
    :link
    TXT,

    'service_due_soon' => <<<'TXT'
    :installer

    :type juaj (:unit) ka servisin më :due.

    Për ta caktuar, përgjigjuni këtij mesazhi ose hapni:
    :link
    TXT,

    'service_due_now' => <<<'TXT'
    :installer

    :type juaj (:unit) e ka servisin sot.

    Për ta caktuar, përgjigjuni këtij mesazhi ose hapni:
    :link
    TXT,

    'warranty_ending' => <<<'TXT'
    :installer

    Garancia e prodhuesit për :unit mbaron më :warranty.

    Të dhënat tuaja mbeten këtu:
    :link
    TXT,

    'booking_ack' => <<<'TXT'
    :installer

    E morëm kërkesën tuaj për servisin e :unit dhe do t'ju kontaktojmë për të caktuar orarin.
    TXT,

    'service_keeps_warranty' => 'Prodhuesi kërkon servis çdo vit për këtë pajisje që garancia të vlejë deri më :warranty. Kjo nuk prek të drejtat tuaja ligjore si blerës.',

    'stop_line' => 'Për t\'i ndalur këto mesazhe: :link',
];
