package ru.pgws.pocketpet;

/** Cute pixel-art creatures: 10 ear types x 10 faces x 10 cheek/accent patterns. */
public final class PetSprites {
    public static final int COUNT=1000, SIZE=24;
    private PetSprites() {}
    private static void r(char[][] a,int x1,int y1,int x2,int y2,char col){
        for(int y=Math.max(0,y1);y<=Math.min(SIZE-1,y2);y++)
            for(int x=Math.max(0,x1);x<=Math.min(SIZE-1,x2);x++)a[y][x]=col;
    }
    private static void e(char[][] a,int x,int y,int rx,int ry,char col){
        for(int yy=Math.max(0,y-ry);yy<=Math.min(SIZE-1,y+ry);yy++)
            for(int xx=Math.max(0,x-rx);xx<=Math.min(SIZE-1,x+rx);xx++)
                if(((double)(xx-x)*(xx-x)/(rx*rx)+(double)(yy-y)*(yy-y)/(ry*ry))<=1.08)a[yy][xx]=col;
    }
    public static String[] create(int genes,int stage,int personality){
        int id=((genes%1000)+1000)%1000, ear=id/100,face=(id/10)%10,mark=id%10;
        char[][] a=new char[SIZE][SIZE];
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++)a[y][x]='.';
        // Small round creatures with large expressive faces and rounded limbs.
        if(stage>PetGame.BABY){ e(a,5,18,3,2,'+');e(a,18,18,3,2,'+'); }
        e(a,7,21,3,2,'+');e(a,16,21,3,2,'+');
        e(a,12,14,stage==PetGame.BABY?8:9,stage==PetGame.BABY?7:8,'+');
        // Ten ear silhouettes, always cute, no spikes on face.
        switch(ear){
            case 0:e(a,6,6,2,6,'+');e(a,18,6,2,6,'+');break;  // long bunny
            case 1:e(a,6,9,4,3,'+');e(a,18,9,4,3,'+');break; // bear
            case 2:e(a,6,7,3,4,'+');e(a,18,7,3,4,'+');break; // cat
            case 3:e(a,12,5,3,4,'+');break; // forelock
            case 4:e(a,6,8,3,3,'+');e(a,18,8,3,3,'+');e(a,12,5,2,3,'+');break;
            case 5:e(a,4,10,3,2,'+');e(a,20,10,3,2,'+');break; // floppy
            case 6:e(a,7,7,2,4,'+');e(a,17,7,2,4,'+');break;
            case 7:e(a,8,5,3,4,'+');e(a,16,5,3,4,'+');break;
            case 8:e(a,6,9,3,2,'+');e(a,18,9,3,2,'+');e(a,12,5,3,3,'+');break;
            default:e(a,5,7,2,5,'+');e(a,19,7,2,5,'+');break;
        }
        // Add a continuous dark outline around the coloured silhouette.
        char[][] shell=new char[SIZE][SIZE];
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++)shell[y][x]=a[y][x];
        for(int y=1;y<SIZE-1;y++)for(int x=1;x<SIZE-1;x++)if(a[y][x]=='+'){
            boolean edge=false;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(Math.abs(dx)+Math.abs(dy)==1&&a[y+dy][x+dx]=='.')edge=true;
            if(edge)shell[y][x]='#';
        }
        a=shell;
        // Visible whites make the eyes friendly instead of a solid black silhouette.
        for(int x: new int[]{8,15}){
            e(a,x,13,2,2,'.');
            r(a,x,12,x,13,'#');
            if(face==4||face==9)r(a,x-1,14,x+1,14,'#');
            if(face==3 && x==8){r(a,7,13,9,13,'#');r(a,8,12,8,13,'+');}
            if(face==5){r(a,x,11,x,11,'#');}
            if(face==6){r(a,x+1,12,x+1,13,'#');}
            if(face==7){r(a,x-1,12,x-1,13,'#');}
            if(face==8){r(a,x-1,13,x-1,13,'#');}
            if(face==1){r(a,x-1,14,x+1,14,'+');}
        }
        // 10 readable mouth expressions, kept in a distinct region.
        switch(face){
            case 0:r(a,11,17,13,17,'#');r(a,12,18,12,18,'#');break;
            case 1:r(a,10,17,14,17,'#');r(a,11,18,13,18,'#');break;
            case 2:r(a,12,17,12,18,'#');break;
            case 3:r(a,11,17,13,17,'#');break;
            case 4:r(a,11,17,13,17,'#');r(a,11,18,11,18,'#');break;
            case 5:r(a,11,17,13,17,'#');r(a,13,18,13,18,'#');break;
            case 6:r(a,12,17,13,18,'#');break;
            case 7:r(a,11,18,13,18,'#');r(a,12,17,12,17,'#');break;
            case 8:r(a,10,18,14,18,'#');break;
            default:r(a,11,17,13,17,'#');r(a,10,16,10,16,'#');r(a,14,16,14,16,'#');break;
        }
        // Distinct visible cheek / hair details. Each mark pattern has its own reserved pixel at 1..10.
        int px=7+mark;
        r(a,px,15,px,15,'#'); // signature lives on the face, no stray pixels
        for(int i=0;i<=mark%3;i++)r(a,5+i,16,5+i,16,'o');
        for(int i=0;i<((mark/3)%3)+1;i++)r(a,17-i,16,17-i,16,'o');
        if(stage>=PetGame.TEEN){r(a,4,18,5,19,'#');r(a,18,18,19,19,'#');}
        if(stage==PetGame.ADULT){r(a,9,22,10,22,'#');r(a,14,22,15,22,'#');}
        if(personality==2){r(a,11,9,13,9,'o');}
        String[] out=new String[SIZE];for(int i=0;i<SIZE;i++)out[i]=new String(a[i]);return out;
    }
}
