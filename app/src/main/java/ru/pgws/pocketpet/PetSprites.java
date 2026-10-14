package ru.pgws.pocketpet;

/**
 * 1000 guaranteed-different monochrome 20x20 pixel pets.
 * ID 0..999 = 10 head shapes x 10 faces x 10 belly patterns.
 * Each visual gene occupies a different part of the sprite, so every
 * combination has its own bitmap, across baby/child/teen/adult stages.
 * No Bandai sprites or copyrighted character artwork are included.
 */
public final class PetSprites {
    public static final int COUNT = 1000;
    private static final int SIZE = 20;
    private PetSprites() {}

    private static void rect(char[][] a, int x1,int y1,int x2,int y2,boolean ink) {
        for(int y=Math.max(0,y1);y<=Math.min(SIZE-1,y2);y++)
            for(int x=Math.max(0,x1);x<=Math.min(SIZE-1,x2);x++)
                a[y][x]=ink?'#':'.';
    }
    private static void dot(char[][] a,int x,int y,boolean ink) {rect(a,x,y,x,y,ink);}

    public static String[] create(int gene, int stage, int personality) {
        int id=gene%COUNT; if(id<0)id+=COUNT;
        int head=id/100, face=(id/10)%10, belly=id%10;
        char[][] a=new char[SIZE][SIZE];
        for(int y=0;y<SIZE;y++) for(int x=0;x<SIZE;x++) a[y][x]='.';

        // Body silhouette occupies rows 5..18. Stages progressively grow limbs.
        rect(a,7,5,12,5,true);rect(a,5,6,14,6,true);
        rect(a,4,7,15,7,true);rect(a,3,8,16,8,true);
        rect(a,2,9,17,13,true);rect(a,3,14,16,14,true);
        rect(a,4,15,15,15,true);rect(a,5,16,14,16,true);
        if(stage>=PetGame.CHILD){
            rect(a,5,17,8,17,true);rect(a,11,17,14,17,true);
        } else {rect(a,6,17,13,17,true);}
        if(stage>=PetGame.TEEN){
            rect(a,5,18,8,18,true);rect(a,11,18,14,18,true);
        }
        if(stage==PetGame.ADULT){
            rect(a,1,12,3,13,true);rect(a,16,12,18,13,true);
        }

        // Distinct head/ear shapes, reserved to rows 0..4 (10 patterns).
        switch(head){
            case 0: rect(a,5,0,6,5,true);rect(a,13,0,14,5,true);break; // rabbit
            case 1: rect(a,3,2,6,4,true);rect(a,13,2,16,4,true);break; // bear
            case 2: rect(a,4,0,4,3,true);rect(a,5,2,6,5,true);rect(a,15,0,15,3,true);rect(a,13,2,14,5,true);break; // devil
            case 3: rect(a,9,0,10,5,true);rect(a,7,0,12,1,true);break; // antenna
            case 4: rect(a,6,1,7,4,true);rect(a,8,0,11,5,true);rect(a,12,2,13,4,true);break; // flame
            case 5: rect(a,3,0,5,2,true);rect(a,4,3,7,5,true);rect(a,14,0,16,2,true);rect(a,12,3,15,5,true);break; // cat
            case 6: rect(a,2,3,7,4,true);rect(a,12,3,17,4,true);rect(a,0,1,4,2,true);rect(a,15,1,19,2,true);break; // fins
            case 7: rect(a,8,0,11,1,true);rect(a,7,2,12,2,true);rect(a,6,3,13,5,true);break; // crest
            case 8: rect(a,2,0,3,2,true);rect(a,8,0,11,2,true);rect(a,16,0,17,2,true);rect(a,3,3,16,5,true);break; // crown
            default:rect(a,1,0,4,1,true);rect(a,4,2,7,5,true);rect(a,12,2,15,5,true);rect(a,15,0,18,1,true);break; // wings
        }

        // 10 face patterns. White pixel cutouts on black body, separate from ears / belly.
        switch(face){
            case 0: // two round dots
                dot(a,6,9,false);dot(a,13,9,false);dot(a,9,11,false);dot(a,10,11,false);break;
            case 1: // large happy eyes
                rect(a,5,9,7,10,false);rect(a,12,9,14,10,false);
                rect(a,8,12,11,12,false);break;
            case 2: // vertical eyes
                rect(a,6,8,6,10,false);rect(a,13,8,13,10,false);
                rect(a,9,12,10,12,false);break;
            case 3: // winking
                rect(a,5,9,7,9,false);rect(a,12,8,13,10,false);dot(a,10,12,false);break;
            case 4: // sleepy closed eyes
                rect(a,5,10,8,10,false);rect(a,11,10,14,10,false);
                dot(a,9,12,false);break;
            case 5: // starry eyes
                dot(a,6,8,false);rect(a,5,9,7,9,false);dot(a,6,10,false);
                dot(a,13,8,false);rect(a,12,9,14,9,false);dot(a,13,10,false);
                rect(a,9,12,10,12,false);break;
            case 6: // surprised
                rect(a,6,9,7,10,false);rect(a,12,9,13,10,false);
                rect(a,9,11,10,12,false);break;
            case 7: // angular
                dot(a,5,8,false);dot(a,6,9,false);dot(a,7,10,false);
                dot(a,14,8,false);dot(a,13,9,false);dot(a,12,10,false);
                rect(a,9,12,10,12,false);break;
            case 8: // round eyes with central brow
                rect(a,5,8,7,10,false);rect(a,12,8,14,10,false);
                dot(a,6,9,true);dot(a,13,9,true);dot(a,10,12,false);break;
            default: // cheeky face
                rect(a,5,9,6,9,false);rect(a,13,9,14,9,false);
                rect(a,4,11,6,11,false);rect(a,13,11,15,11,false);
                rect(a,9,12,10,12,false);break;
        }

        // 10 distinct lower-torso markings on rows 14..16.
        switch(belly){
            case 0: rect(a,9,14,10,16,false);break;
            case 1: dot(a,7,15,false);dot(a,12,15,false);break;
            case 2: rect(a,7,14,12,14,false);break;
            case 3: dot(a,7,14,false);dot(a,12,14,false);rect(a,8,15,11,15,false);dot(a,9,16,false);dot(a,10,16,false);break;
            case 4: rect(a,6,15,8,15,false);rect(a,11,15,13,15,false);break;
            case 5: rect(a,8,14,11,16,false);rect(a,9,15,10,15,true);break;
            case 6: dot(a,7,14,false);dot(a,9,15,false);dot(a,11,14,false);dot(a,13,15,false);break;
            case 7: rect(a,6,16,13,16,false);break;
            case 8: rect(a,8,14,8,16,false);rect(a,11,14,11,16,false);break;
            default:dot(a,6,14,false);dot(a,8,15,false);dot(a,10,16,false);dot(a,12,15,false);dot(a,14,14,false);break;
        }
        // Care-dependent variation: stressed adults have drooping paws.
        if(stage==PetGame.ADULT && personality==2){
            rect(a,1,14,2,16,true);rect(a,17,14,18,16,true);
        }
        String[] out=new String[SIZE];
        for(int y=0;y<SIZE;y++)out[y]=new String(a[y]);
        return out;
    }
}
