import React, { useState } from 'react';
import { View, Text, StyleSheet, Dimensions, TouchableOpacity } from 'react-native';
import Svg, { Path, Defs, LinearGradient, Stop, Circle, Line, Text as SvgText, G, Rect } from 'react-native-svg';

interface LineChartProps {
    data: number[];
    labels: string[];
    color?: string;
    gradientColors?: string[];
    height?: number;
    showDots?: boolean;
    showValues?: boolean;
}

export function LineChart({
    data,
    labels,
    color = '#2E7D32',
    gradientColors = ['rgba(46, 125, 50, 0.15)', 'rgba(46, 125, 50, 0)'],
    height = 120,
    showDots = true,
}: LineChartProps) {
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
    const width = Dimensions.get('window').width - 64; // Account for padding
    const padding = { top: 30, bottom: 30, left: 20, right: 20 };
    const chartHeight = height - padding.top - padding.bottom;
    const chartWidth = width - padding.left - padding.right;

    const maxValue = Math.max(...data);
    const minValue = Math.min(...data);
    const range = maxValue - minValue || 1;

    // Calculate points
    const points = data.map((value, index) => ({
        x: padding.left + (index / (data.length - 1)) * chartWidth,
        y: padding.top + chartHeight - ((value - minValue) / range) * chartHeight,
        value,
    }));

    // Create line path
    const linePath = points
        .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`)
        .join(' ');

    // Create area path (closed shape for gradient fill)
    const areaPath = `${linePath} L ${points[points.length - 1].x} ${padding.top + chartHeight} L ${points[0].x} ${padding.top + chartHeight} Z`;

    // Generate unique ID for this chart instance
    const gradientId = `areaGradient-${Math.random().toString(36).substr(2, 9)}`;

    const handlePress = (index: number) => {
        setSelectedIndex(index === selectedIndex ? null : index);
    };

    return (
        <View style={[styles.container, { height }]}>
            <Svg width={width} height={height}>
                <Defs>
                    <LinearGradient id={gradientId} x1="0%" y1="0%" x2="0%" y2="100%">
                        <Stop offset="0%" stopColor={gradientColors[0]} stopOpacity="0.2" />
                        <Stop offset="100%" stopColor={gradientColors[1]} stopOpacity="0" />
                    </LinearGradient>
                </Defs>

                {/* Grid lines */}
                {[0, 0.25, 0.5, 0.75, 1].map((ratio, index) => (
                    <Line
                        key={index}
                        x1={padding.left}
                        y1={padding.top + chartHeight * ratio}
                        x2={width - padding.right}
                        y2={padding.top + chartHeight * ratio}
                        stroke="#E0E0E0"
                        strokeWidth={1}
                        strokeDasharray="3,3"
                    />
                ))}

                {/* Area fill */}
                <Path d={areaPath} fill={`url(#${gradientId})`} />

                {/* Line */}
                <Path
                    d={linePath}
                    stroke={color}
                    strokeWidth={2.5}
                    fill="none"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />

                {/* Interactive Dots */}
                {showDots && points.map((point, index) => {
                    const isSelected = selectedIndex === index;
                    const isLast = index === points.length - 1;
                    const showValueForPoint = isSelected || (selectedIndex === null && isLast);

                    return (
                        <G key={index} onPress={() => handlePress(index)}>
                            {/* Larger transparent touch target */}
                            <Circle
                                cx={point.x}
                                cy={point.y}
                                r={20}
                                fill="transparent"
                            />
                            {/* Visible dot */}
                            <Circle
                                cx={point.x}
                                cy={point.y}
                                r={isSelected ? 6 : 4}
                                fill="#FFFFFF"
                                stroke={color}
                                strokeWidth={2}
                            />
                            {/* Value Label */}
                            {showValueForPoint && (
                                <G>
                                    <Circle
                                        cx={point.x}
                                        cy={point.y - 16}
                                        r={14}
                                        fill={color}
                                    />
                                    <SvgText
                                        x={point.x}
                                        y={point.y - 12}
                                        fill="#FFFFFF"
                                        fontSize={10}
                                        fontWeight="600"
                                        textAnchor="middle"
                                    >
                                        {Math.round(point.value / 1000)}k
                                    </SvgText>
                                </G>
                            )}
                        </G>
                    );
                })}

                {/* X-axis labels */}
                {labels.map((label, index) => (
                    <SvgText
                        key={index}
                        x={padding.left + (index / (labels.length - 1)) * chartWidth}
                        y={height - 8}
                        fill="#757575"
                        fontSize={10}
                        textAnchor="middle"
                    >
                        {label}
                    </SvgText>
                ))}
            </Svg>
        </View>
    );
}

interface DualLineChartProps {
    data1: number[];
    data2: number[];
    labels: string[];
    color1?: string;
    color2?: string;
    height?: number;
}

export function DualLineChart({
    data1,
    data2,
    labels,
    color1 = '#2E7D32',
    color2 = '#F57C00',
    height = 120,
}: DualLineChartProps) {
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
    const width = Dimensions.get('window').width - 64;
    const padding = { top: 30, bottom: 30, left: 20, right: 20 };
    const chartHeight = height - padding.top - padding.bottom;
    const chartWidth = width - padding.left - padding.right;

    // Unique IDs for gradients
    const gradientId1 = `gradient1-${Math.random().toString(36).substr(2, 9)}`;
    const gradientId2 = `gradient2-${Math.random().toString(36).substr(2, 9)}`;

    const allData = [...data1, ...data2];
    const maxValue = Math.max(...allData);
    const minValue = Math.min(...allData);
    const range = maxValue - minValue || 1;

    const getPoints = (data: number[]) => data.map((value, index) => ({
        x: padding.left + (index / (data.length - 1)) * chartWidth,
        y: padding.top + chartHeight - ((value - minValue) / range) * chartHeight,
        value,
    }));

    const points1 = getPoints(data1);
    const points2 = getPoints(data2);

    const createLinePath = (points: typeof points1) =>
        points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');

    const createAreaPath = (points: typeof points1) =>
        `${createLinePath(points)} L ${points[points.length - 1].x} ${padding.top + chartHeight} L ${points[0].x} ${padding.top + chartHeight} Z`;

    const handlePress = (index: number) => {
        setSelectedIndex(index === selectedIndex ? null : index);
    };

    return (
        <View style={[styles.container, { height }]}>
            <Svg width={width} height={height}>
                <Defs>
                    <LinearGradient id={gradientId1} x1="0%" y1="0%" x2="0%" y2="100%">
                        <Stop offset="0%" stopColor={color1} stopOpacity="0.2" />
                        <Stop offset="100%" stopColor={color1} stopOpacity="0" />
                    </LinearGradient>
                    <LinearGradient id={gradientId2} x1="0%" y1="0%" x2="0%" y2="100%">
                        <Stop offset="0%" stopColor={color2} stopOpacity="0.2" />
                        <Stop offset="100%" stopColor={color2} stopOpacity="0" />
                    </LinearGradient>
                </Defs>

                {/* Grid lines */}
                {[0, 0.5, 1].map((ratio, index) => (
                    <Line
                        key={index}
                        x1={padding.left}
                        y1={padding.top + chartHeight * ratio}
                        x2={width - padding.right}
                        y2={padding.top + chartHeight * ratio}
                        stroke="#E0E0E0"
                        strokeWidth={1}
                        strokeDasharray="3,3"
                    />
                ))}

                {/* Area fills */}
                <Path d={createAreaPath(points1)} fill={`url(#${gradientId1})`} />
                <Path d={createAreaPath(points2)} fill={`url(#${gradientId2})`} />

                {/* Lines */}
                <Path d={createLinePath(points1)} stroke={color1} strokeWidth={2.5} fill="none" strokeLinecap="round" />
                <Path d={createLinePath(points2)} stroke={color2} strokeWidth={2.5} fill="none" strokeLinecap="round" />

                {/* Interactive Dots and values */}
                {(points1.length > 0 ? points1 : points2).map((_, index) => {
                    const isSelected = selectedIndex === index;
                    const isLast = index === points1.length - 1;
                    const showValueForPoint = isSelected || (selectedIndex === null && isLast);

                    // Collision detection for labels
                    const p1 = points1[index];
                    const p2 = points2[index];
                    const distance = Math.abs(p1.y - p2.y);
                    const offset = 16;
                    // If too close, push labels away from each other
                    const isTooClose = distance < 30;
                    const y1Label = isTooClose && p1.y < p2.y ? p1.y - offset - 10 : p1.y - offset;
                    const y2Label = isTooClose && p2.y < p1.y ? p2.y - offset - 10 : (isTooClose ? p2.y + offset + 10 : p2.y - offset);

                    // Ensure Y2 label doesn't go below chart
                    const safeY2Label = y2Label > height - 15 ? p2.y - offset : y2Label;

                    return (
                        <G key={index} onPress={() => handlePress(index)}>
                            {/* Larger transparent touch target covering both lines vertical space potentially */}
                            <Rect
                                x={points1[index].x - 15}
                                y={0}
                                width={30}
                                height={height}
                                fill="transparent"
                            />

                            {/* Visible dots for both lines */}
                            <Circle
                                cx={points1[index].x}
                                cy={points1[index].y}
                                r={isSelected ? 6 : 4}
                                fill="#FFFFFF"
                                stroke={color1}
                                strokeWidth={2}
                            />
                            <Circle
                                cx={points2[index].x}
                                cy={points2[index].y}
                                r={isSelected ? 6 : 4}
                                fill="#FFFFFF"
                                stroke={color2}
                                strokeWidth={2}
                            />

                            {/* Value Labels */}
                            {showValueForPoint && (
                                <G>
                                    {/* Label for Line 1 */}
                                    <Circle cx={points1[index].x} cy={y1Label - 4} r={14} fill={color1} />
                                    <SvgText x={points1[index].x} y={y1Label} fill="#FFFFFF" fontSize={10} fontWeight="600" textAnchor="middle">
                                        {Math.round(points1[index].value / 1000)}k
                                    </SvgText>

                                    {/* Label for Line 2 */}
                                    <Circle cx={points2[index].x} cy={safeY2Label - 4} r={14} fill={color2} />
                                    <SvgText x={points2[index].x} y={safeY2Label} fill="#FFFFFF" fontSize={10} fontWeight="600" textAnchor="middle">
                                        {Math.round(points2[index].value / 1000)}k
                                    </SvgText>
                                </G>
                            )}
                        </G>
                    );
                })}

                {/* X-axis labels */}
                {labels.map((label, index) => (
                    <SvgText
                        key={index}
                        x={padding.left + (index / (labels.length - 1)) * chartWidth}
                        y={height - 8}
                        fill="#757575"
                        fontSize={10}
                        textAnchor="middle"
                    >
                        {label}
                    </SvgText>
                ))}
            </Svg>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        justifyContent: 'center',
    },
});
