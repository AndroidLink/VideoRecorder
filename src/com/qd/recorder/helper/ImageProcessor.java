package com.qd.recorder.helper;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_features2d.*;

/**
 * Created by yangfeng on 14/12/27.
 */
public class ImageProcessor {
    //平滑
    public static void smooth(String filename) {
        IplImage image = cvLoadImage(filename);
        if (image != null) {
//            cvSmooth(image, image, CV_GAUSSIAN, 3);
            cvSmooth(image, image);
            cvSaveImage(filename, image);
            cvReleaseImage(image);
        }
    }

    //增加外框
    public static void padding(String filename)
    {
        CvMat src,dst;
        int top=10,  left=10;
        int borderType=IPL_BORDER_CONSTANT;
        CvScalar value;
        /// Load an image
        src = cvLoadImageM(filename);
        //dst =cvCreateImage( cvSize( src.cvSize().width()+left, src.cvSize().height()+top ), IPL_DEPTH_8U, 3 ).asCvMat();
        dst =  cvCreateMat(src.rows()+left,src.cols()+top,CV_8UC3);
//        value = new CvScalar(0,0,0,0);
        value = new CvScalar(0);
        //point为src 在dst图像上的左上角坐标
        CvPoint point=cvPoint(left/2,top/2);
        cvCopyMakeBorder( src, dst, point, borderType, value );
        cvSaveImage(filename, dst);
    }

    //金字塔放大
    public static void pyramid_up(String filename)
    {
        CvMat src, dst, tmp;
        /// Load an image
        src = cvLoadImageM(filename);
        //tmp = src;
        dst = cvCreateMat(src.rows()*2,src.cols()*2,src.type());
        cvPyrUp( src, dst, CV_GAUSSIAN_5x5);
        //cvPyrDown( tmp, dst,2);
        //tmp = dst;
        cvSaveImage(filename, dst);
    }

    //金字塔缩小
    public static void pyramid_down(String filename)
    {
        CvMat src, dst, tmp;
        /// Load an image
        src = cvLoadImageM(filename);
        //tmp = src;
        dst = cvCreateMat(src.rows()/2,src.cols()/2,src.type());
        cvPyrDown( src, dst, CV_GAUSSIAN_5x5);
        //cvPyrDown( tmp, dst,2);
        // tmp = dst;
        cvSaveImage(filename, dst);
    }

    //扩张，将目标的边缘的“毛刺”踢除掉
    public static void morphology_Dilation(String filename, int dilation_elem)
    {
        CvMat src, dilation_dst;
        src = cvLoadImageM(filename);
        dilation_dst=src;
        int dilation_type=CV_SHAPE_RECT;
        if( dilation_elem == 0 ){ dilation_type = CV_SHAPE_RECT; }
        else if( dilation_elem == 1 ){ dilation_type = CV_SHAPE_CROSS; }
        else if( dilation_elem == 2) { dilation_type = CV_SHAPE_ELLIPSE; }
//    CvMat element = cvGetStructuringElement( dilation_type,
//    cvSize( 2*1 + 1, 2*1+1 ),
//    cvPoint( 2, 2 ) );
        /// Apply the dilation operation
//        IplConvKernel kernel=cvCreateStructuringElementEx(3,3,1,1,dilation_type,null);
        IplConvKernel kernel=cvCreateStructuringElementEx(3,3,1,1,dilation_type);
        cvDilate( src, dilation_dst, kernel,1);
        cvReleaseStructuringElement( kernel );
        cvSaveImage(filename, dilation_dst);
    }

    //侵蚀，将目标的边缘或者是内部的坑填掉
    public static void morphology_Erosion(String filename,int dilation_elem)
    {
        CvMat src, erosion_dst;
        src = cvLoadImageM(filename);
        erosion_dst=src;
        int dilation_type=CV_SHAPE_RECT;
        if( dilation_elem == 0 ){ dilation_type = CV_SHAPE_RECT; }
        else if( dilation_elem == 1 ){ dilation_type = CV_SHAPE_CROSS; }
        else if( dilation_elem == 2) { dilation_type = CV_SHAPE_ELLIPSE; }
//    CvMat element = cvGetStructuringElement( dilation_type,
//    cvSize( 2*1 + 1, 2*1+1 ),
//    cvPoint( 2, 2 ) );
        /// Apply the dilation operation
//        IplConvKernel kernel=cvCreateStructuringElementEx(3,3,1,1,dilation_type,null);
        IplConvKernel kernel=cvCreateStructuringElementEx(3,3,1,1,dilation_type);
        cvErode( src, erosion_dst, kernel,1);
        cvReleaseStructuringElement( kernel );
        cvSaveImage(filename, erosion_dst);
    }
    /**
     *
     * @date Jul 3, 2012 10:51:47 AM
     * @author suntengjiao@ceopen.cn
     * @desc 最简单图像分割方法
     * @param filename
     */
    //The simplest segmentation method
    public static void thresholding(String filename,int type)
    {
        IplImage src, pGrayImg,dst;
        int threshold_value = 0;
    /* 0: Binary
    1: Binary Inverted
    2: Threshold Truncated
    3: Threshold to Zero
    4: Threshold to Zero Inverted
    */
        int threshold_type = type;
        int  max_BINARY_value = 255;
        src=cvLoadImage(filename);
        pGrayImg=gray(src);
        dst=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1);
        /**
         * – src_gray: Our input image
         – dst: Destination (output) image
         – threshold_value: The thresh value with respect to which the thresholding operation is made
         – max_BINARY_value: The value used with the Binary thresholding operations (to set the chosen pixels)
         – threshold_type: One of the 5 thresholding operations.
         */
        cvThreshold( pGrayImg, dst, threshold_value, max_BINARY_value,threshold_type );
        cvSaveImage(filename,dst);
        cvReleaseImage(src);
        cvReleaseImage(dst);
        cvReleaseImage(pGrayImg);
    }

    /**
     *
     * @date Jul 3, 2012 10:52:51 AM
     * @author suntengjiao@ceopen.cn
     * @desc 计算灰度图
     * @param src
     * @return
     */
    public static IplImage gray(IplImage src)
    {
        //将RGB色彩空间转换成BGR色彩空间 8位 3通道
        IplImage  pImg = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 3);
      /*src是源图像；
        dst是转换后的图像；
        flags是转换的模式，可以取0：没有变化；1：垂直翻转，即沿x轴翻转；2：交换红蓝信道；
      */
        cvConvertImage(src, pImg, 2);


        //将RGB转换成Gray度图
        IplImage  pGrayImg = cvCreateImage(
                cvGetSize(pImg),
                IPL_DEPTH_8U,
                1);
        cvCvtColor(pImg, pGrayImg, CV_RGB2GRAY);
        cvReleaseImage(pImg);
        return pGrayImg;
        //cvSaveImage("D:\\IBM\\gray.jpg",pGrayImg);
    }

    public static void gray(String filename) {
        IplImage image = cvLoadImage(filename);
        IplImage grayImage = gray(image);
        if (grayImage != null) {
            cvSaveImage(filename, grayImage);
            cvReleaseImage(image);
            cvReleaseImage(grayImage);
        }
    }

    //sobel边缘检测
    public static void sobel(String filename)
    {
        CvMat src, src_gray;
        CvMat grad=null;
        int scale = 1;
        int delta = 0;
        int ddepth = CV_16S;
        int c;
        src=cvLoadImageM(filename);
        //GaussianBlur( src, src, cvSize(3,3), 0, 0, BORDER_DEFAULT );
//        cvSmooth(src, src, CV_GAUSSIAN, 3);
        cvSmooth(src, src);
        src_gray=gray(src.asIplImage()).asCvMat();
        CvMat grad_x=null, grad_y=null;
        CvMat abs_grad_x=null, abs_grad_y=null;
        /// Gradient X
        //Scharr( src_gray, grad_x, ddepth, 1, 0, scale, delta, BORDER_DEFAULT );
        grad_x=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        cvSobel( src_gray, grad_x, 1, 0, 3);
        abs_grad_x=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        /**
         * src ： 原数组
         　　dst ：输出数组 (深度为 8u).
         　　scale ：比例因子.
         　　shift ：原数组元素按比例缩放后添加的值
         */
        cvConvertScaleAbs( grad_x, abs_grad_x,1,0);
        /// Gradient Y
        //Scharr( src_gray, grad_y, ddepth, 0, 1, scale, delta, BORDER_DEFAULT );
        grad_y=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        abs_grad_y=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        cvSobel( src_gray, grad_y, 0, 1, 3);
        cvConvertScaleAbs( grad_y, abs_grad_y,1,0 );
        /// Total Gradient (approximate)
        grad=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        cvAddWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0,grad );
        cvSaveImage(filename,grad);

    }
    /**
     *
     * @date Jul 3, 2012 10:51:47 AM
     * @author suntengjiao@ceopen.cn
     * @desc 拉普拉斯边缘检测
     * @param filename
     */
    public static void laplacian(String filename)
    {
        CvMat src, src_gray,dst,abs_dst;
        src=cvLoadImageM(filename);
//        cvSmooth(src, src, CV_GAUSSIAN, 3);
        cvSmooth(src, src);
        src_gray=gray(src.asIplImage()).asCvMat();
        dst=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        abs_dst=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
        /**
         * src_gray  输入图像
         * dst 输出图像
         * 3 核大小
         */
        cvLaplace( src_gray, dst,3);
        cvConvertScaleAbs( dst, abs_dst,1,0);
//        cvWaitKey(0);
    }
    /**
     *
     * @date Jul 3, 2012 10:51:47 AM
     * @author suntengjiao@ceopen.cn
     * @desc canny边缘检测
     * @param filename
     */
    public static void canny(String filename)
    {

        CvMat src,src_gray, detected_edges,dst;
        src=cvLoadImageM(filename);
        src_gray=gray(src.asIplImage()).asCvMat();
        detected_edges=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();
//        cvSmooth(src_gray, detected_edges, CV_GAUSSIAN, 3);
        cvSmooth(src_gray, detected_edges);
        /**
         * image 输入图像，这个必须是单通道的，即灰度图
         　　edges 输出的边缘图像 ，也是单通道的，但是是黑白的
         　　threshold1 第一个阈值
         　　threshold2 第二个阈值
         　　aperture_size Sobel 算子内核大小
         * canny算子得实质：如果一个像素的梯度大与上限值，
         * 则被认为是边缘像素，如果小于下限阈值，则被抛弃，
         * 那么如果该店的梯度位于两者之间呢？则当其与高于上限值的像素点连接时我们才保留，否则删除。
         */
        cvCanny(detected_edges, detected_edges, 90, 90 * 3, 3);
        //dst = Scalar::all(0);
        dst=cvCreateMat(src.rows(),src.cols(), src.type());
        //cvSetIdentity(dst,cvRealScalar(0));
        cvSet(dst, cvScalar(0,0,0,0),null);
        cvCopy( src,dst,detected_edges);
        cvSaveImage(filename,dst);
    }
    /**
     *
     * @date Jul 3, 2012 10:51:15 AM
     * @author suntengjiao@ceopen.cn
     * @desc 标准哈夫变换直线
     * @param filename
     */
    public static void standardHoughLine(String filename)
    {
        CvMat src, detected_edges,color_dst;
        src=cvLoadImageM(filename,0);//加载灰度图
        detected_edges=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();//创建一通道图像
        color_dst = cvCreateImage( cvGetSize( src ), IPL_DEPTH_8U, 3).asCvMat();  //创建三通道图像
        //cvSmooth(src, detected_edges, CV_GAUSSIAN, 3);
        //边缘检测
        cvCanny(src, detected_edges,50,200,3);
        //src_gray=gray(detected_edges.asIplImage()).asCvMat();
        cvCvtColor( detected_edges, color_dst, CV_GRAY2BGR ); //色彩空间转换，将dst转换到另外一个色彩空间即3通道图像

        CvMemStorage storage=cvCreateMemStorage(0);
     /*
      * detected_edges：8-bit, single-channel binary source image
      * storage：存储检测到得线段
      * method：CV_HOUGH_STANDARD(标准哈夫变换) ，CV_HOUGH_PROBABILISTIC(矩阵必须是CV_32SC4类型)，CV_HOUGH_MULTI_SCALE
      * rho  与像素相关单位的距离精度
           * theta 弧度测量的角度精度
           * threshold 累计阀值
      * param1：当method=CV_HOUGH_STANDARD 设置为0表示不需要，当method=CV_HOUGH_PROBABILISTIC 表示最小线长度，当method=CV_HOUGH_MULTI_SCALE 意义还不清楚
      * param2：当method=CV_HOUGH_STANDARD 设置为0表示不需要，当method=CV_HOUGH_PROBABILISTIC 表示线的最大空隙；大于此空隙算作两个线，当method=CV_HOUGH_MULTI_SCALE 意义还不清楚
      */
        CvSeq lines  = cvHoughLines2( detected_edges, storage, CV_HOUGH_STANDARD, 1, Math.PI/180, 150, 0, 0);
        //循环直线序列
        for( int i = 0; i < lines.total(); i++ )
        {
            FloatPointer line=new FloatPointer(cvGetSeqElem(lines,i));//用GetSeqElem获得直线
//    CvPoint2D32f point = new CvPoint2D32f(cvGetSeqElem(lines, i));
//
//                   float rho=point.x();
//                float theta=point.y();
            //对于SHT和MSHT（标准变换）这里line[0]，line[1]是rho（与像素相干单位的距离精度）和theta（弧度测量的角度精度）
            float rho = line.get(0);
            float theta =line.get(1);
            System.out.println(rho+"::"+theta);
            CvPoint pt1, pt2;
            float a = (float)Math.cos(theta), b = (float)Math.sin(theta);
            float x0 = a*rho, y0 = b*rho;
            //pt1.position(0).x(Math.round(x0 + 1000*(-b))) ;
            //pt1.position(0).y(Math.round(y0 + 1000*(a)));
            pt1=new CvPoint();
            pt1.x(Math.round(x0 + 1000*(-b)));
            pt1.y(Math.round(y0 + 1000*(a)));
            //pt2.position(0).x(Math.round(x0 - 1000*(-b)));
            //pt2.position(0).y(Math.round(y0 - 1000*(a)));
            pt2=new CvPoint();
            pt2.x(Math.round(x0 - 1000*(-b)));
            pt2.y(Math.round(y0 - 1000*(a)));
            cvLine(color_dst, pt1, pt2, CV_RGB(0, 0, 255), 1, CV_AA, 0);
        }
//        cvNamedWindow("Hough");
//        cvShowImage( "Hough", color_dst );
//        cvWaitKey();
    }
    /**
     *
     * @date Jul 3, 2012 10:50:37 AM
     * @author suntengjiao@ceopen.cn
     * @desc 哈夫变换直线
     * @param filename
     */
    //The Probabilistic Hough Line Transform
    public static void houghLine(String filename)
    {
        CvMat src, detected_edges,color_dst;
        src=cvLoadImageM(filename,0);//加载灰度图
        detected_edges=cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 1).asCvMat();//创建一通道图像
        color_dst = cvCreateImage( cvGetSize( src ), IPL_DEPTH_8U, 3 ).asCvMat();  //创建三通道图像
        //cvSmooth(src, detected_edges, CV_GAUSSIAN, 3);
        //边缘检测
        cvCanny(src, detected_edges,50,200,3);
        //src_gray=gray(detected_edges.asIplImage()).asCvMat();
        cvCvtColor( detected_edges, color_dst, CV_GRAY2BGR ); //色彩空间转换，将dst转换到另外一个色彩空间即3通道图像

        CvMemStorage storage=cvCreateMemStorage(0);
     /*
      * detected_edges：8-bit, single-channel binary source image
      * storage：存储检测到得线段
      * method：CV_HOUGH_STANDARD(标准哈夫变换) ，CV_HOUGH_PROBABILISTIC(矩阵必须是CV_32SC4类型)，CV_HOUGH_MULTI_SCALE
      * rho  与像素相关单位的距离精度
           * theta 弧度测量的角度精度
           * threshold 累计阀值
      * param1：当method=CV_HOUGH_STANDARD 设置为0表示不需要，当method=CV_HOUGH_PROBABILISTIC 表示最小线长度，当method=CV_HOUGH_MULTI_SCALE 意义还不清楚
      * param2：当method=CV_HOUGH_STANDARD 设置为0表示不需要，当method=CV_HOUGH_PROBABILISTIC 表示线的最大空隙；大于此空隙算作两个线，当method=CV_HOUGH_MULTI_SCALE 意义还不清楚
      */
        CvSeq lines  = cvHoughLines2( detected_edges, storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI/180, 150, 50, 10);
        //循环直线序列
        for( int i = 0; i < lines.total(); i++ )  //lines存储的是直线
        {
            CvPoint line = new CvPoint(cvGetSeqElem(lines,i));
            /**
             *  img – Image.
             pt1 – First point of the line segment.
             pt2 – Second point of the line segment.
             color – Line color.
             thickness – Line thickness.
             lineType –
             Type of the line:
             8 (or omitted) - 8-connected line.
             4 - 4-connected line.
             CV_AA - antialiased line.
             shift – Number of fractional bits in the point coordinates.
             */
            cvLine( color_dst,new CvPoint(line.position(0)),new CvPoint(line.position(1)), CV_RGB( 0, 255, 0 ),1,CV_AA,0 );  //将找到的直线标记为红色
            //color_dst是三通道图像用来存直线图像
        }
//        cvNamedWindow("Hough");
//        cvShowImage( "Hough", color_dst );
//        cvWaitKey();
        //Canvas.showImage(color_dst);
        // cvSaveImage("D:\\IBM\\houghLineCV_AA.jpg",color_dst);
    }

    /**
     *
     * @date Jul 2, 2012 3:58:29 PM
     * @author suntengjiao@ceopen.cn
     * @desc 哈夫变换，检测圆
     * @param filename
     */
    public static void houghCircle(String filename){
        CvMat src, src_gray,color_dst;
        src=cvLoadImageM(filename);//加载灰度图
        src_gray=gray(src.asIplImage()).asCvMat();
//        cvSmooth(src_gray, src_gray, CV_GAUSSIAN, 3);
        cvSmooth(src_gray, src_gray);
        CvMemStorage storage=cvCreateMemStorage(0);
        /// Apply the Hough Transform to find the circles
        /**
         image
         输入 8-比特、单通道灰度图像.
         circle_storage
         检测到的圆存储仓. 可以是内存存储仓 (此种情况下，一个线段序列在存储仓中被创建，并且由函数返回）或者是包含圆参数的特殊类型的具有单行/单列的CV_32FC3型矩阵(CvMat*). 矩阵头为函数所修改，使得它的 cols/rows 将包含一组检测到的圆。如果 circle_storage 是矩阵，而实际圆的数目超过矩阵尺寸，那么最大可能数目的圆被返回
         . 每个圆由三个浮点数表示：圆心坐标(x,y)和半径.

         method
         Hough 变换方式，目前只支持CV_HOUGH_GRADIENT, which is basically 21HT, described in [Yuen03].
         dp
         累加器图像的分辨率。这个参数允许创建一个比输入图像分辨率低的累加器。（这样做是因为有理由认为图像中存在的圆会自然降低到与图像宽高相同数量的范畴）。如果dp设置为1，则分辨率是相同的；如果设置为更大的值（比如2），累加器的分辨率受此影响会变小（此情况下为一半）。dp的值不能比1小。
         Resolution of the accumulator used to detect centers of the circles. For example, if it is 1, the accumulator will have the same resolution as the input image, if it is 2 - accumulator will have twice smaller width and height, etc.

         min_dist
         该参数是让算法能明显区分的两个不同圆之间的最小距离。
         Minimum distance between centers of the detected circles. If the parameter is too small, multiple neighbor circles may be falsely detected in addition to a true one. If it is too large, some circles may be missed.

         param1
         用于Canny的边缘阀值上限，下限被置为上限的一半。
         The first method-specific parameter. In case of CV_HOUGH_GRADIENT it is the higher threshold of the two passed to Canny edge detector (the lower one will be twice smaller).

         param2
         累加器的阀值。
         The second method-specific parameter. In case of CV_HOUGH_GRADIENT it is accumulator threshold at the center detection stage. The smaller it is, the more false circles may be detected. Circles, corresponding to the larger accumulator values, will be returned first.

         min_radius
         最小圆半径。
         Minimal radius of the circles to search for.

         max_radius
         最大圆半径。
         */
        CvSeq circles=cvHoughCircles( src_gray, storage, CV_HOUGH_GRADIENT, 1, src.rows()/8, 200, 100, 0, 0 );
        /// Draw the circles detected
        for( int i = 0; i < circles.total(); i++ )
        {
            FloatPointer seq=new FloatPointer(cvGetSeqElem(circles,i));
            System.out.println(seq.get(0)+","+seq.get(1)+","+seq.get(2));
            CvPoint center=new CvPoint ();
            center.x(Math.round(seq.get(0)));
            center.y(Math.round(seq.get(1)));
            int radius = Math.round(seq.get(2));
            // circle center
            cvCircle( src, center, 3, CV_RGB(0,255,0), -1, 8, 0 );
            // circle outline
            cvCircle( src, center, radius, CV_RGB(255,0,0), 3, 8, 0 );
        }
        /// Show your results
//        cvNamedWindow( "Hough Circle Transform Demo", CV_WINDOW_AUTOSIZE );
//        cvShowImage( "Hough Circle Transform Demo", src );
//        cvWaitKey(0);

    }
    /**
     *
     * @date Jul 2, 2012 3:57:45 PM
     * @author suntengjiao@ceopen.cn
     * @desc 直方图均衡化，增强图像的对比度
     * @param filename
     */
    public static void  histogramEqualization(String filename)
    {
        CvMat src,src_gray, detected_edges,dst;
        src=cvLoadImageM(filename);
        src_gray=gray(src.asIplImage()).asCvMat();
        dst =  cvCreateMat(src.rows(),src.cols(),CV_8UC1);
        /// Apply Histogram Equalization
        cvEqualizeHist( src_gray, dst );
        /// Display results
//        cvNamedWindow( "source_window", CV_WINDOW_AUTOSIZE );
//        cvNamedWindow( "equalized_window", CV_WINDOW_AUTOSIZE );
//        cvShowImage( "source_window", src );
//        cvShowImage( "equalized_window", dst );
        /// Wait until user exits the program
//        cvWaitKey(0);
    }
    /**
     *
     * @date Jul 3, 2012 10:49:50 AM
     * @author suntengjiao@ceopen.cn
     * @desc 直方图计算,归一化参数为1
     */
    public static void histogramCalculationFor1(String filename)
    {
        CvMat src,redImage,greenImage,blueImage;
        src=cvLoadImageM(filename);
        redImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();
        greenImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();
        blueImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();

        cvSplit(src,blueImage,greenImage,redImage,null);
        IplImage  b_planes[] = {blueImage.asIplImage()};
        IplImage  g_planes[] = {greenImage.asIplImage()};
        IplImage  r_planes[] = {redImage.asIplImage()};
        /// Establish the number of bins
        int histSize = 256;
        /// Set the ranges ( for B,G,R) )
        float range[] = { 0, 255} ;
        float[] histRange[] = { range };
        int hist_size[] = {histSize};
        /**
         * param1:直方图维数，
         * param2:bin的个数，
         * param3:直方图表示格式，CV_HIST_ARRAY（多维浓密数组），CV_HIST_SPARSE（多维稀疏数组）
         * param4:param5为1时，ranges[i](0<=i<cDims，译者注：cDims为直方图的维数)是包含两个元素的范围数组，包括直方图第i维的上界和下界。
         * 在第i维上的整个区域 [lower,upper]被分割成 dims[i]（译者注：dims[i]表示直方图第i维的bin数） 个相等的bin;
         * param5为0时,则ranges[i]是包含dims[i]+1个元素的范围数组，
         * 包括lower0, upper0, lower1, upper1 == lower2, ..., upper(dims[i]-1),
         *  其中lowerj 和upperj分别是直方图第i维上第 j 个bin的上下界（针对输入象素的第 i 个值）
         * param5:归一化标识，0或1
         *
         */
        CvHistogram b_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,1);
        /// Compute the histograms:
        /**
         * param1:输入图像
         * param2:直方图
         * param3:累计标识,如果设置，则直方图在开始时不被清零。这个特征保证可以为多个图像计算一个单独的直方图，或者在线更新直方图。
         * param4:mask ,操作 mask, 确定输入图像的哪个象素被计数
         */
        cvCalcHist(b_planes,b_hist,0,null);

        CvHistogram g_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,1);
        /// Compute the histograms:
        cvCalcHist(g_planes,g_hist,0,null);
        CvHistogram r_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,1);
        /// Compute the histograms:
        cvCalcHist(r_planes,r_hist,0,null);

        // Draw the histograms for B, G and R
        int hist_w = 512; int hist_h = 400;
        int bin_w = Math.round(hist_w/histSize );
        CvMat histImage=cvCreateMat(hist_h, hist_w, CV_8UC3);
        cvSet(histImage,CV_RGB( 0,0,0),null);
        ///归一化， Normalize the result to [ 0, histImage.rows ]
        cvNormalize(b_hist.mat(), b_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        cvNormalize(g_hist.mat(),g_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        cvNormalize(r_hist.mat(),r_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        /// Draw for each channel
        for( int i = 1; i < histSize; i++ )
        {
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h -(int)(Math.round( cvGetReal1D( b_hist.bins(), i-1 ))) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( b_hist.mat(),i)) ),
                    CV_RGB( 255, 0, 0), 2, 8, 0 );
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h - (int)Math.round(cvGetReal1D( g_hist.bins(),i-1)) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( g_hist.bins(),i)) ),
                    CV_RGB( 0, 255, 0), 2, 8, 0 );
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h - (int)Math.round(cvGetReal1D( r_hist.bins(),i-1)) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( r_hist.bins(),i)) ),
                    CV_RGB( 0, 0, 255), 2, 8, 0 );
        }
        /// Display
//        cvNamedWindow("calcHist Demo", CV_WINDOW_AUTOSIZE );
//        cvShowImage("calcHist Demo", histImage );
//        cvWaitKey(0);
    }
    /**
     *
     * @date Jul 3, 2012 10:49:50 AM
     * @author suntengjiao@ceopen.cn
     * @desc 直方图计算，归一化参数为0
     */
    public static void histogramCalculationFor0(String filename)
    {
        CvMat src,redImage,greenImage,blueImage;
        src=cvLoadImageM(filename);
        redImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();
        greenImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();
        blueImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();

        cvSplit(src,blueImage,greenImage,redImage,null);
        IplImage  b_planes[] = {blueImage.asIplImage()};
        IplImage  g_planes[] = {greenImage.asIplImage()};
        IplImage  r_planes[] = {redImage.asIplImage()};
        /// Establish the number of bins
        int histSize = 3;
        /// Set the ranges ( for B,G,R) )
        float range[] = { 0, 100,101,200,201,255} ;
        float[] histRange[] = { range };
        int hist_size[] = {histSize};
        /**
         * param1:直方图维数，
         * param2:bin的个数，
         * param3:直方图表示格式，CV_HIST_ARRAY（多维浓密数组），CV_HIST_SPARSE（多维稀疏数组）
         * param4:param5为1时，ranges[i](0<=i<cDims，译者注：cDims为直方图的维数)是包含两个元素的范围数组，包括直方图第i维的上界和下界。
         * 在第i维上的整个区域 [lower,upper]被分割成 dims[i]（译者注：dims[i]表示直方图第i维的bin数） 个相等的bin;
         * param5为0时,则ranges[i]是包含dims[i]+1个元素的范围数组，
         * 包括lower0, upper0, lower1, upper1 == lower2, ..., upperdims[i]-1,
         *  其中lowerj 和upperj分别是直方图第i维上第 j 个bin的上下界（针对输入象素的第 i 个值）
         * param5:归一化标识，0或1
         *
         */
        CvHistogram b_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,0);
        /// Compute the histograms:
        cvCalcHist(b_planes,b_hist,0,null);

        CvHistogram g_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,0);
        /// Compute the histograms:
        cvCalcHist(g_planes,g_hist,0,null);
        CvHistogram r_hist = cvCreateHist(1,hist_size,CV_HIST_ARRAY,histRange,0);
        /// Compute the histograms:
        cvCalcHist(r_planes,r_hist,0,null);

        // Draw the histograms for B, G and R
        int hist_w = 512; int hist_h = 400;
        int bin_w = Math.round(hist_w/histSize );
        //CvMat histImage=new CvMat( hist_h, hist_w, CV_8UC3);
        //dst = Scalar::all(0);
        CvMat histImage=cvCreateMat(hist_h, hist_w, CV_8UC3);
        //cvSetIdentity(dst,cvRealScalar(0));
        cvSet(histImage,CV_RGB( 0,0,0),null);
        /// Normalize the result to [ 0, histImage.rows ]
        cvNormalize(b_hist.mat(), b_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        cvNormalize(g_hist.mat(),g_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        cvNormalize(r_hist.mat(),r_hist.mat(), 1, histImage.rows(), NORM_MINMAX ,null);
        /// Draw for each channel
        for( int i = 1; i < histSize; i++ )
        {
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h -(int)(Math.round( cvGetReal1D( b_hist.bins(), i-1 ))) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( b_hist.mat(),i)) ),
                    CV_RGB( 255, 0, 0), 2, 8, 0 );
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h - (int)Math.round(cvGetReal1D( g_hist.bins(),i-1)) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( g_hist.bins(),i)) ),
                    CV_RGB( 0, 255, 0), 2, 8, 0 );
            cvLine( histImage, cvPoint( bin_w*(i-1), hist_h - (int)Math.round(cvGetReal1D( r_hist.bins(),i-1)) ) ,
                    cvPoint( bin_w*(i), hist_h - (int)Math.round(cvGetReal1D( r_hist.bins(),i)) ),
                    CV_RGB( 0, 0, 255), 2, 8, 0 );
        }
        /// Display
//        cvNamedWindow("calcHist Demo", CV_WINDOW_AUTOSIZE );
//        cvShowImage("calcHist Demo", histImage );
//        cvWaitKey(0);
    }

    public static void histogramComparison(String src,String test1,String test2)
    {
        CvMat src_base,src_half,src_test1,src_test2,hsv_base,hsv_test1,hsv_test2,hsv_half_down;
        src_base=cvLoadImageM(src);
        src_test1=cvLoadImageM(test1);
        src_test2=cvLoadImageM(test2);
        System.out.println("坐标起始点:"+ src_base.asIplImage().origin());
        src_half=cvCreateMatHeader(src_test2.rows()/2,src_test2.cols(),src_test2.type());
        CvRect rect=cvRect(0,  src_base.rows()/2, src_base.cols(), src_base.rows()/2);
        cvGetSubRect(src_base,src_half, rect);
        hsv_base = cvCreateImage(cvGetSize(src_base),8,3).asCvMat();
        hsv_test1 = cvCreateImage(cvGetSize(src_test1),8,3).asCvMat();
        hsv_test2 = cvCreateImage(cvGetSize(src_test2),8,3).asCvMat();
        hsv_half_down=cvCreateImage(cvGetSize(src_half),8,3).asCvMat();
        /// Convert to HSV
        cvCvtColor( src_base, hsv_base, CV_BGR2HSV );
        cvCvtColor( src_test1, hsv_test1, CV_BGR2HSV );
        cvCvtColor( src_test2, hsv_test2, CV_BGR2HSV );
        cvCvtColor( src_half, hsv_half_down, CV_BGR2HSV );
        //获得h和s通道的值
        IplImage hsv_base_h_plane = cvCreateImage(cvGetSize(src_base),8,1);
        IplImage hsv_base_s_plane = cvCreateImage(cvGetSize(src_base),8,1);
        cvSplit(hsv_base,hsv_base_h_plane,hsv_base_s_plane,null,null);
        IplImage[] hsv_base_array={hsv_base_h_plane,hsv_base_s_plane};
        //获得h和s通道的值
        IplImage hsv_test1_h_plane = cvCreateImage(cvGetSize(src_test1),8,1);
        IplImage hsv_test1_s_plane = cvCreateImage(cvGetSize(src_test1),8,1);
        cvSplit(hsv_test1,hsv_test1_h_plane,hsv_test1_s_plane,null,null);
        IplImage[] hsv_test1_array={hsv_test1_h_plane,hsv_test1_s_plane};
        //获得h和s通道的值
        IplImage hsv_test2_h_plane = cvCreateImage(cvGetSize(src_test2),8,1);
        IplImage hsv_test2_s_plane = cvCreateImage(cvGetSize(src_test2),8,1);
        cvSplit(hsv_test2,hsv_test2_h_plane,hsv_test2_s_plane,null,null);
        IplImage[] hsv_test2_array={hsv_test2_h_plane,hsv_test2_s_plane};
        //获得h和s通道的值
        IplImage hsv_half_down_h_plane = cvCreateImage(cvGetSize(hsv_half_down),8,1);
        IplImage hsv_half_down_s_plane = cvCreateImage(cvGetSize(hsv_half_down),8,1);
        cvSplit(hsv_half_down,hsv_half_down_h_plane,hsv_half_down_s_plane,null,null);
        IplImage[] hsv_half_down_array={hsv_half_down_h_plane,hsv_half_down_s_plane};


        /// Using 30 bins for hue and 32 for saturation
        int h_bins = 50; int s_bins = 60;
        int histSize[] = { h_bins, s_bins };
        // hue varies from 0 to 256, saturation from 0 to 180
        float h_ranges[] = { 0, 256 };
        float s_ranges[] = { 0, 180 };
        float[] ranges[] = { h_ranges, s_ranges };
        // Use the o-th and 1-st channels
        //redImage=cvCreateImage(cvGetSize(src),IPL_DEPTH_8U,1).asCvMat();
        CvHistogram hist_base = cvCreateHist(2,histSize,CV_HIST_ARRAY,ranges,1);
        /// Compute the histograms:
        cvCalcHist(hsv_base_array,hist_base,0,null);
        cvNormalize(hist_base.mat(), hist_base.mat(), 0,1, NORM_MINMAX ,null);

        CvHistogram hist_half_down = cvCreateHist(2,histSize,CV_HIST_ARRAY,ranges,1);
        /// Compute the histograms:
        cvCalcHist(hsv_half_down_array,hist_half_down,0,null);
        cvNormalize(hist_half_down.mat(), hist_half_down.mat(), 0,1, NORM_MINMAX ,null);

        CvHistogram hist_test1 = cvCreateHist(2,histSize,CV_HIST_ARRAY,ranges,1);
        /// Compute the histograms:
        cvCalcHist(hsv_test1_array,hist_test1,0,null);
        cvNormalize(hist_test1.mat(), hist_test1.mat(), 0,1, NORM_MINMAX ,null);

        CvHistogram hist_test2 = cvCreateHist(2,histSize,CV_HIST_ARRAY,ranges,1);
        /// Compute the histograms:
        cvCalcHist(hsv_test2_array,hist_test2,0,null);
        cvNormalize(hist_test2.mat(), hist_test2.mat(), 0,1, NORM_MINMAX ,null);

        for( int i = 0; i < 4; i++ )
        {
            int compare_method = i;
            double base_base = cvCompareHist( hist_base, hist_base, compare_method );
            double base_half = cvCompareHist( hist_base, hist_half_down, compare_method );
            double base_test1 = cvCompareHist( hist_base, hist_test1, compare_method );
            double base_test2 = cvCompareHist( hist_base, hist_test2, compare_method );
            System.out.println( " Method [%d] Perfect, Base-Half, Base-Test(1)," +
                    " Base-Test(2) : %f, %f, %f, %f \n"+i+","+ base_base+","+ base_half+","+base_test1+","+base_test2);
        }
    }
    /**
     *
     * @date Jul 6, 2012 4:39:19 PM
     * @author suntengjiao@ceopen.cn
     * @desc 反向投影
     * @param filename
     * @param bins
     */
    public static void backProjection(String filename,int bins){
        /**
         *  filename – Name of file to be loaded.
         flags –
         Flags specifying the color type of a loaded image:
         >0 Return a 3-channel color image
         =0 Return a grayscale image
         <0 Return the loaded image as is
         */

        IplImage target=cvLoadImage(filename,1);  //装载图片
        IplImage target_hsv=cvCreateImage( cvGetSize(target), IPL_DEPTH_8U, 3 );
        IplImage target_hue=cvCreateImage( cvGetSize(target), IPL_DEPTH_8U, 1);
        cvCvtColor(target,target_hsv,CV_BGR2HSV);       //转化到HSV空间
        cvSplit( target_hsv, target_hue, null, null, null );    //获得H分量
//    int ch[] = { 0, 0 };
//    IplImage[] target_hsv_s={target_hsv};
//    IplImage[] target_hue_s={target_hue};
//    cvMixChannels(target_hsv_s, 1, target_hue_s, 1, ch, 1 );
        if(bins<2) bins=2;
        int hist_size[]={bins};          //将H分量的值量化到[0,255]
        float[] ranges[]={ {0,180} };    //H分量的取值范围是[0,360)
        CvHistogram hist=cvCreateHist(1, hist_size, CV_HIST_ARRAY,ranges, 1);
        IplImage[] target_hues={target_hue};
        cvCalcHist(target_hues,hist, 0, null);
        cvNormalize(hist.mat(), hist.mat(), 0,255, NORM_MINMAX ,null);
        IplImage result=cvCreateImage(cvGetSize(target),IPL_DEPTH_8U,1);

        cvCalcBackProject(target_hues,result,hist);
//        cvShowImage( "BackProj", result );
        //cvShowImage( "src", target );
//        cvWaitKey(0);
    }

    /**
     *
     * @date Jul 6, 2012 5:33:11 PM
     * @author suntengjiao@ceopen.cn
     * @desc 模版匹配
     * @param src
     * @param template
     * @param match_method
     */
    public static void templateMatching(String src,String template,int match_method)
    {
        CvMat img=cvLoadImageM(src,1);  //装载图片
        CvMat templ=cvLoadImageM(template,1);  //装载图片
        /// Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        CvMat result=cvCreateMat( result_cols, result_rows, CV_32FC1 );
        cvMatchTemplate( img, templ, result, 0 );
        cvNormalize(result, result, 0,1, NORM_MINMAX ,null);
        /// Localizing the best match with minMaxLoc
//        double minVal[]=new double[5];
//        double maxVal[]=new double[5];
        DoublePointer minVal = new DoublePointer();
        DoublePointer maxVal = new DoublePointer();
        CvPoint minLoc=new CvPoint();
        CvPoint maxLoc=new CvPoint();
        CvPoint matchLoc=new CvPoint();
        cvMinMaxLoc(result, minVal, maxVal, minLoc, maxLoc,null);
        /// For SQDIFF and SQDIFF_NORMED, the best matches are lower values. For all the other methods, the higher the better
        if( match_method == CV_TM_SQDIFF || match_method == CV_TM_SQDIFF_NORMED )
        { matchLoc = minLoc; }
        else
        { matchLoc = maxLoc; }
        /// Source image to display
        CvMat img_display=cvCreateMat( img.cols(),img.rows(), img.type() );;
        cvCopy(img,img_display,null);
        /// Show me what you got
        CvPoint tempLoc = new CvPoint();
        tempLoc.x(matchLoc.x() + templ.cols()); // matchLoc.x() + templ.cols()
        tempLoc.y(matchLoc.y() + templ.rows()); // matchLoc.y() + templ.rows()
        CvScalar scalar = new CvScalar();
        cvRectangle( img_display, matchLoc, tempLoc, scalar, 2, 8, 0);
        cvRectangle( result, matchLoc, tempLoc, scalar, 2, 8, 0 );
//        cvShowImage( "image_window", img_display );
//        cvShowImage( "result_window", result );
    }
}
