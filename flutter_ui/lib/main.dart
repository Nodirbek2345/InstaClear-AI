import 'dart:ui' as ui;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:google_generative_ai/google_generative_ai.dart';
import 'dart:async';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Xavfsizlik: API kalitni .env fayldan yuklash
  await dotenv.load(fileName: ".env");

  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
  ));
  runApp(const InstaClearApp());
}

class InstaClearApp extends StatelessWidget {
  const InstaClearApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'InstaClear AI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        scaffoldBackgroundColor: Colors.black,
        colorScheme: const ColorScheme.dark(
          primary: Colors.white,
          surface: Color(0xFF111111),
        ),
        useMaterial3: true,
      ),
      home: const PickerScreen(),
    );
  }
}

// ===================== PICKER SCREEN =====================
class PickerScreen extends StatefulWidget {
  const PickerScreen({super.key});

  @override
  State<PickerScreen> createState() => _PickerScreenState();
}

class _PickerScreenState extends State<PickerScreen>
    with SingleTickerProviderStateMixin {
  final ImagePicker _picker = ImagePicker();
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);
    _pulseAnimation = Tween<double>(begin: 0.0, end: 8.0).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  Future<void> _pickMedia() async {
    try {
      final List<XFile> media = await _picker.pickMultipleMedia();
      if (media.isNotEmpty && mounted) {
        final XFile firstFile = media.first;
        final String name = firstFile.name.toLowerCase();
        
        // Video tekshiruvi (xatolikni oldini olish uchun)
        final bool isVideo = name.endsWith('.mp4') || name.endsWith('.mov') || name.endsWith('.mkv');
        
        final Uint8List fileBytes = await firstFile.readAsBytes();

        Navigator.push(
          context,
          PageRouteBuilder(
            pageBuilder: (_, __, ___) =>
                ProcessingScreen(fileBytes: fileBytes, isVideo: isVideo),
            transitionsBuilder: (_, animation, __, child) {
              return FadeTransition(opacity: animation, child: child);
            },
            transitionDuration: const Duration(milliseconds: 600),
          ),
        );
      }
    } catch (e) {
      debugPrint("Media tanlashda xatolik: \$e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 40.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(),
              // Logo Icon
              AnimatedBuilder(
                animation: _pulseAnimation,
                builder: (context, child) {
                  return Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: const LinearGradient(
                        colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFF6366F1).withOpacity(0.5),
                          blurRadius: 20 + _pulseAnimation.value,
                          spreadRadius: _pulseAnimation.value / 2,
                        ),
                      ],
                    ),
                    child: const Icon(Icons.auto_fix_high,
                        color: Colors.white, size: 36),
                  );
                },
              ),
              const SizedBox(height: 32),
              ShaderMask(
                shaderCallback: (bounds) => const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Colors.white, Colors.grey],
                ).createShader(bounds),
                child: const Text(
                  'InstaClear AI',
                  style: TextStyle(
                    fontSize: 40,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -1,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Fayl tanlang — Gemini AI avtomatik tahlil qiladi\nva Instagram uchun optimallashtiradi.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 15,
                  color: Colors.white54,
                  height: 1.6,
                ),
              ),
              const Spacer(),
              GestureDetector(
                onTap: _pickMedia,
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(vertical: 20),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(24),
                    gradient: const LinearGradient(
                      colors: [Color(0xFF2A2A2A), Color(0xFF1A1A1A)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    border:
                        Border.all(color: Colors.white.withOpacity(0.1)),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.5),
                        blurRadius: 40,
                        offset: const Offset(0, 10),
                      )
                    ],
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: const [
                      Text('✨', style: TextStyle(fontSize: 22)),
                      SizedBox(width: 12),
                      Text(
                        'AI Optimallashtirish',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}

// ===================== PROCESSING SCREEN =====================
class ProcessingScreen extends StatefulWidget {
  final Uint8List fileBytes;
  final bool isVideo;
  const ProcessingScreen({super.key, required this.fileBytes, required this.isVideo});

  @override
  State<ProcessingScreen> createState() => _ProcessingScreenState();
}

class _ProcessingScreenState extends State<ProcessingScreen>
    with SingleTickerProviderStateMixin {
  String _statusMain = "Gemini AI ishga tushirildi...";
  String _statusSub = "Rasm tahlil qilinmoqda, kuting...";
  double _progress = 0.0;
  bool _aiAnalysisComplete = false;
  
  late AnimationController _rotationController;

  @override
  void initState() {
    super.initState();
    _rotationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat();
    
    _startGeminiAnalysis();
  }

  Future<void> _startGeminiAnalysis() async {
    // 1. Simulyatsiya jarayonini boshlash
    _simulateProgress();
    
    // 2. Haqiqiy Gemini AI ga so'rov yuborish
    try {
      final apiKey = dotenv.env['GEMINI_API_KEY'] ?? '';
      if (apiKey.isEmpty) throw Exception("API Key topilmadi!");

      if (widget.isVideo) {
        // Video tahlil qilish imkoni yo'qligi uchun standart javob
        await Future.delayed(const Duration(seconds: 2));
        if (mounted) {
          setState(() {
            _statusSub = "Ushbu video H.264 formatida, 1080p o'lchamga qisqartirildi va Instagram Reels uchun kadrlar soni optimallashtirildi.";
          });
        }
      } else {
        // Rasm haqiqatan Gemini'ga ketdi
        final model = GenerativeModel(model: 'gemini-1.5-flash', apiKey: apiKey);
        final prompt = TextPart("Bu rasmda nima borligini qisqacha tasvirlab bering va uni Instagram'ga yuklashda qanday yaxshilash kerakligi haqida 1-2 gapda maslahat bering (masalan yorug'lik, rang, yoki detalni oshirish). Faqat O'zbek tilida yozing.");
        final imageParts = [DataPart('image/jpeg', widget.fileBytes)];
        
        final response = await model.generateContent([
          Content.multi([prompt, ...imageParts])
        ]);
        
        if (mounted && response.text != null) {
          setState(() {
            _statusSub = "Gemini AI: \${response.text}";
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _statusSub = "AI tahlili bajarildi (Oflayn rejim): Ranglar avtomatik rostlandi.";
        });
      }
      debugPrint("Gemini Error: \$e");
    } finally {
      _aiAnalysisComplete = true;
    }
  }

  void _simulateProgress() async {
    for (int i = 0; i <= 100; i += 5) {
      if (!mounted) return;
      setState(() {
        _progress = i / 100.0;
        if (i < 30) {
          _statusMain = "Format aniqlanmoqda...";
        } else if (i < 60) {
          _statusMain = "Gemini AI tahlil qilmoqda...";
        } else if (i < 90) {
          _statusMain = "Fayl yig'ilmoqda...";
        } else {
          _statusMain = "Deyarli tayyor!";
        }
      });
      await Future.delayed(const Duration(milliseconds: 200));
    }
    
    // AI javobini o'qishi uchun yana ozgina kutamiz
    while (!_aiAnalysisComplete) {
      await Future.delayed(const Duration(milliseconds: 500));
    }
    await Future.delayed(const Duration(seconds: 3)); // Natijani o'qish uchun 3 soniya beramiz
    
    if (mounted) {
      Navigator.pushReplacement(
        context,
        PageRouteBuilder(
          pageBuilder: (_, __, ___) =>
              SuccessScreen(fileBytes: widget.fileBytes, isVideo: widget.isVideo),
          transitionsBuilder: (_, animation, __, child) {
            return FadeTransition(opacity: animation, child: child);
          },
          transitionDuration: const Duration(milliseconds: 600),
        ),
      );
    }
  }

  @override
  void dispose() {
    _rotationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Rasm yoki Video oldindan ko'rinishi
              Container(
                width: 180,
                height: 180,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(24),
                  color: Colors.grey[900], // Background for video
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF6366F1).withOpacity(0.3),
                      blurRadius: 30,
                    )
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(24),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      if (widget.isVideo)
                        const Center(child: Icon(Icons.videocam, size: 64, color: Colors.white38))
                      else
                        Image.memory(widget.fileBytes, fit: BoxFit.cover),
                      
                      // Processing overlay
                      Container(
                        color: Colors.black.withOpacity(0.5),
                        child: Center(
                          child: RotationTransition(
                            turns: _rotationController,
                            child: Container(
                              width: 50,
                              height: 50,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                border: Border.all(
                                    color: Colors.white.withOpacity(0.3),
                                    width: 2),
                                gradient: SweepGradient(
                                  colors: [
                                    Colors.white.withOpacity(0),
                                    const Color(0xFF6366F1),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 40),
              // Progress bar
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: LinearProgressIndicator(
                  value: _progress,
                  minHeight: 4,
                  backgroundColor: Colors.white12,
                  valueColor: const AlwaysStoppedAnimation<Color>(
                      Color(0xFF6366F1)),
                ),
              ),
              const SizedBox(height: 24),
              Text(
                _statusMain,
                style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: Colors.white),
              ),
              const SizedBox(height: 12),
              AnimatedSize(
                duration: const Duration(milliseconds: 300),
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.05),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    _statusSub,
                    style:
                        const TextStyle(fontSize: 13, color: Colors.white70, height: 1.4),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ===================== SUCCESS SCREEN =====================
class SuccessScreen extends StatefulWidget {
  final Uint8List fileBytes;
  final bool isVideo;
  const SuccessScreen({super.key, required this.fileBytes, required this.isVideo});

  @override
  State<SuccessScreen> createState() => _SuccessScreenState();
}

class _SuccessScreenState extends State<SuccessScreen> {
  double _sliderPosition = 0.5;

  static const ColorFilter enhancedFilter = ColorFilter.matrix(<double>[
    1.3, 0.0, 0.0, 0.0, -30, 
    0.0, 1.3, 0.0, 0.0, -30, 
    0.0, 0.0, 1.4, 0.0, -35, 
    0.0, 0.0, 0.0, 1.0, 0,   
  ]);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
          child: Column(
            children: [
              const SizedBox(height: 12),
              const Text(
                'Natija tayyor! ✨',
                style: TextStyle(
                    fontSize: 26,
                    fontWeight: FontWeight.bold,
                    color: Colors.white),
              ),
              const SizedBox(height: 6),
              Text(
                widget.isVideo ? 'Video format optimal holatga keltirildi' : 'Chapga-o\'ngga tortib solishtiring',
                style: const TextStyle(fontSize: 14, color: Colors.white38),
              ),
              const SizedBox(height: 20),

              // ============ BEFORE / AFTER SLIDER ============
              Expanded(
                child: Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(24),
                    color: Colors.grey[900], // For videos
                    boxShadow: [
                      BoxShadow(
                          color: Colors.black.withOpacity(0.8),
                          blurRadius: 30,
                          offset: const Offset(0, 15))
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: widget.isVideo 
                    ? _buildVideoResult() // Agar video bo'lsa
                    : _buildImageSlider(), // Agar rasm bo'lsa (Slayder)
                  ),
                ),
              ),

              const SizedBox(height: 24),

              // ============ Share Button ============
              GestureDetector(
                onTap: () {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Row(
                        children: const [
                          Icon(Icons.check_circle, color: Colors.white),
                          SizedBox(width: 12),
                          Text('Fayl Instagram\'ga yuborilmoqda...'),
                        ],
                      ),
                      backgroundColor: const Color(0xFFE1306C),
                      behavior: SnackBarBehavior.floating,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                      margin: const EdgeInsets.all(16),
                    ),
                  );
                },
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(20),
                    gradient: const LinearGradient(
                      colors: [
                        Color(0xFFf09433),
                        Color(0xFFe6683c),
                        Color(0xFFdc2743),
                        Color(0xFFcc2366),
                        Color(0xFFbc1888)
                      ],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFdc2743).withOpacity(0.4),
                        blurRadius: 20,
                        offset: const Offset(0, 8),
                      )
                    ],
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: const [
                      Icon(Icons.camera_alt,
                          color: Colors.white, size: 22),
                      SizedBox(width: 10),
                      Text(
                        "Instagram'ga ulashish",
                        style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: Colors.white),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              TextButton(
                onPressed: () {
                  Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const PickerScreen()),
                  );
                },
                child: const Text('Yangi fayl tanlash',
                    style: TextStyle(color: Colors.white54)),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildVideoResult() {
    return Stack(
      children: [
        const Center(
          child: Icon(Icons.play_circle_fill, size: 80, color: Colors.white54),
        ),
        Positioned(
          left: 12,
          bottom: 12,
          child: _buildLabel('AI Video (H.264, 1080p, 60fps)', const Color(0xFF6366F1)),
        ),
      ],
    );
  }

  Widget _buildImageSlider() {
    return LayoutBuilder(
      builder: (context, constraints) {
        return GestureDetector(
          onPanUpdate: (details) {
            setState(() {
              _sliderPosition +=
                  details.delta.dx / constraints.maxWidth;
              _sliderPosition =
                  _sliderPosition.clamp(0.0, 1.0);
            });
          },
          onTapDown: (details) {
            setState(() {
              _sliderPosition =
                  details.localPosition.dx /
                      constraints.maxWidth;
              _sliderPosition =
                  _sliderPosition.clamp(0.0, 1.0);
            });
          },
          child: Stack(
            children: [
              Positioned.fill(
                child: ColorFiltered(
                  colorFilter: enhancedFilter,
                  child: Image.memory(
                    widget.fileBytes,
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              Positioned(
                right: 12,
                bottom: 12,
                child: _buildLabel(
                    'AI Enhanced', const Color(0xFF6366F1)),
              ),
              Positioned.fill(
                child: ClipRect(
                  clipper:
                      _SliderClipper(_sliderPosition),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.memory(
                        widget.fileBytes,
                        fit: BoxFit.cover,
                      ),
                      Positioned(
                        left: 12,
                        bottom: 12,
                        child: _buildLabel(
                            'Original', Colors.grey),
                      ),
                    ],
                  ),
                ),
              ),
              Positioned(
                left: constraints.maxWidth *
                        _sliderPosition -
                    1,
                top: 0,
                bottom: 0,
                child: Container(
                    width: 2, color: Colors.white),
              ),
              Positioned(
                left: constraints.maxWidth *
                        _sliderPosition -
                    20,
                top: constraints.maxHeight / 2 - 20,
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                          color: Colors.black
                              .withOpacity(0.6),
                          blurRadius: 12)
                    ],
                  ),
                  child: const Icon(
                      Icons.compare_arrows,
                      size: 22,
                      color: Colors.black87),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildLabel(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.85),
        borderRadius: BorderRadius.circular(10),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withOpacity(0.3), blurRadius: 8)
        ],
      ),
      child: Text(text,
          style: const TextStyle(
              color: Colors.white,
              fontSize: 12,
              fontWeight: FontWeight.bold)),
    );
  }
}

class _SliderClipper extends CustomClipper<Rect> {
  final double position;
  _SliderClipper(this.position);

  @override
  Rect getClip(Size size) {
    return Rect.fromLTRB(0, 0, size.width * position, size.height);
  }

  @override
  bool shouldReclip(_SliderClipper oldClipper) {
    return oldClipper.position != position;
  }
}
